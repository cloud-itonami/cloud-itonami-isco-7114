(ns concretecrew.governor
  "ConcreteCrewGovernor — the independent safety/traceability layer
  named in this repository's README/business-model.md, gating every
  job-site scheduling/logistics coordination proposal an advisor may
  make. The governor never dispatches hardware itself, never performs
  concrete work itself, and never lets a proposal finalize a
  concrete-pour/finishing-execution decision or override a
  site-safety officer's judgment — those are hard, permanent blocks,
  not merely high-risk escalations. Modeled on
  cloud-itonami-isco-3313's accountingsupport.governor and
  cloud-itonami-isco-7115's carpentry.governor.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable —
  including by human approval; a hard block is not the same node as
  the `:request-approval` interrupt and is never reachable from it):
    1. client provenance      — the contractor/crew company must be
                                registered.
    2. closed op-allowlist    — `:op` must be one of
                                `:log-work-record`,
                                `:schedule-crew-operation`,
                                `:flag-safety-concern` or
                                `:coordinate-supply-order`. No other
                                op exists in this actor — in
                                particular there is no op that
                                finalizes a concrete pour, finalizes
                                finishing work, or overrides a site
                                safety officer, by construction.
    3. no-actuation            — proposal `:effect` must be
                                `:propose` (the governor never
                                dispatches hardware and this actor
                                never performs concrete work itself;
                                it only coordinates scheduling and
                                logistics around it).
    4. scope exclusion         — a proposal whose text content
                                indicates it would finalize a
                                concrete-pour/finishing-execution
                                decision, or override a site safety
                                officer's judgment, is a hard,
                                PERMANENT block regardless of op,
                                confidence or stake. This is a second
                                independent line of defense on top of
                                the closed op-allowlist (rule 2), in
                                case a future op or a manipulated
                                rationale/task field tries to smuggle
                                such an instruction through an
                                otherwise-allowed op.
    5. site provenance          — a proposal must cite a REGISTERED
                                site belonging to this client.
    6. site independent verification — the cited site must be
                                INDEPENDENTLY VERIFIED
                                (`:verified?` true, e.g. a
                                site-safety officer's sign-off) — a
                                site record merely existing is not
                                enough.
    7. worker provenance/verification (crew operations only) — a
                                `:schedule-crew-operation` proposal
                                must cite a REGISTERED, independently
                                verified worker belonging to this
                                client.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off per
  business-model.md's Trust Controls — these are :high/
  :safety-critical regardless of confidence):
    8. :op :flag-safety-concern (a surfaced site-condition/timing/
                                equipment concern always goes to a
                                human — the governor never resolves
                                it itself).
    9. :op :coordinate-supply-order whose :cost exceeds the site's
                                registered :max-supply-cost.
    10. low confidence (< `confidence-floor`).

  Scope-exclusion phrasing note: the term list below is written as
  full finalization/execution ACTION phrases ('proceed with the
  concrete pour', 'override the site safety officer's judgment'),
  never as bare nouns ('pour', 'safety', 'concrete'). This actor's OWN
  legitimate vocabulary routinely contains those bare nouns — crew
  scheduling proposals mention pour-timing, safety-concern proposals
  mention safety, supply orders mention concrete — so a bare-noun term
  list would false-positive-match the actor's own default rationale
  text and self-block ordinary coordination proposals. See
  `governor-test/default-mock-advisor-proposals-never-self-trip-scope-exclusion`
  in governor_test.clj, which asserts every default mock-advisor
  proposal for all four allowlisted ops passes this check clean."
  (:require [concretecrew.store :as store]
            [clojure.string :as str]))

(def confidence-floor 0.6)

(def ^:private allowed-ops
  #{:log-work-record :schedule-crew-operation :flag-safety-concern
    :coordinate-supply-order})

(def ^:private always-escalate-ops #{:flag-safety-concern})

;; See the "Scope-exclusion phrasing note" in the namespace docstring:
;; full ACTION phrases only, never bare nouns.
(def ^:private scope-exclusion-phrases
  ["proceed with the concrete pour"
   "finalize the concrete pour"
   "finalize the pour"
   "finalize the finishing work"
   "authorize the concrete pour"
   "override the site safety officer's judgment"
   "override the safety officer's judgment"
   "override the site safety officer"])

(defn- text-blob [proposal]
  (str/lower-case
   (str/join " " (keep proposal [:rationale :task :concern :progress :instruction :notes]))))

(defn- scope-excluded? [proposal]
  (let [blob (text-blob proposal)]
    (boolean (some #(str/includes? blob %) scope-exclusion-phrases))))

(defn- hard-violations [{:keys [request proposal]} client-record si w]
  (let [{:keys [op cost]} proposal
        crew-op? (= :schedule-crew-operation op)
        allowlisted? (contains? allowed-ops op)]
    (cond-> []
      (nil? client-record)
      (conj {:rule :no-client :detail "未登録 client"})

      (not allowlisted?)
      (conj {:rule :op-not-allowlisted
             :detail (str "closed op-allowlist 違反: " (pr-str op)
                          " はこの actor の許可 op 一覧に無い")})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation
             :detail "effect は :propose のみ許可（governor / actor は現場作業を直接実行しない）"})

      (scope-excluded? proposal)
      (conj {:rule :scope-exclusion
             :detail "コンクリート打設・仕上げの最終判断を直接実行する提案、または現場安全責任者の判断を上書きする提案は恒久的に禁止"})

      (nil? si)
      (conj {:rule :unknown-site :detail "未登録 site への提案は不可"})

      (and si (not= (:client-id si) (:client-id request)))
      (conj {:rule :site-wrong-client :detail "site が別 client のもの"})

      (and si (not (:verified? si)))
      (conj {:rule :site-not-verified
             :detail "独立検証（現場安全責任者等のサインオフ）未完了の site への提案は不可"})

      (and crew-op? (nil? w))
      (conj {:rule :unknown-worker :detail "未登録 worker への crew operation 提案は不可"})

      (and crew-op? w (not= (:client-id w) (:client-id request)))
      (conj {:rule :worker-wrong-client :detail "worker が別 client のもの"})

      (and crew-op? w (not (:verified? w)))
      (conj {:rule :worker-not-verified
             :detail "独立検証未完了の worker への crew operation 提案は不可"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `concretecrew.store/Store`. Pure — never
  mutates the store, never dispatches hardware, never performs
  concrete work."
  [request context proposal store]
  (let [client-record (store/client store (:client-id request))
        si (some->> (:site-id proposal) (store/site store))
        w (some->> (:worker-id proposal) (store/worker store))
        hard (hard-violations {:request request :proposal proposal} client-record si w)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        cost-over? (and (= :coordinate-supply-order (:op proposal))
                         si (number? (:cost proposal))
                         (number? (:max-supply-cost si))
                         (> (:cost proposal) (:max-supply-cost si)))
        always-risky? (or (contains? always-escalate-ops (:op proposal)) cost-over?)]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
