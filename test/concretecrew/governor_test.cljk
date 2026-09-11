(ns concretecrew.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [concretecrew.store :as store]
            [concretecrew.advisor :as advisor]
            [concretecrew.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Concrete Crew"})
    (store/register-site! st {:site-id "S-1" :client-id "client-1"
                               :name "site-042" :verified? true
                               :max-supply-cost 5000})
    (store/register-worker! st {:worker-id "W-1" :client-id "client-1"
                                 :name "worker-042" :verified? true})
    st))

(def ^:private req {:client-id "client-1"})

(defn- log-op []
  {:op :log-work-record :effect :propose :site-id "S-1"
   :task "footing pour prep" :materials-usage {"aggregate" 12} :progress "50%"
   :confidence 0.9 :stake :low})

(defn- crew-op []
  {:op :schedule-crew-operation :effect :propose :site-id "S-1" :worker-id "W-1"
   :task "finishing crew" :scheduled-time "2026-07-20T08:00:00Z"
   :confidence 0.9 :stake :low})

(defn- flag-op []
  {:op :flag-safety-concern :effect :propose :site-id "S-1"
   :concern "wet weather delaying cure" :confidence 0.9 :stake :low})

(defn- supply-op [cost]
  {:op :coordinate-supply-order :effect :propose :site-id "S-1"
   :material "ready-mix concrete" :quantity 8 :cost cost
   :confidence 0.9 :stake :low})

(deftest ok-within-allowlist-and-verified-site
  (let [st (fresh-store)
        v (governor/check req {} (log-op) st)]
    (is (:ok? v))))

(deftest ok-crew-operation-with-verified-site-and-worker
  (let [st (fresh-store)
        v (governor/check req {} (crew-op) st)]
    (is (:ok? v))))

(deftest ok-at-exact-supply-cost-ceiling-boundary
  (testing "the supply-cost ceiling is inclusive"
    (let [st (fresh-store)
          v (governor/check req {} (supply-op 5000) st)]
      (is (:ok? v)))))

(deftest escalates-supply-order-above-cost-ceiling
  (testing "supply orders above the site's registered cost ceiling always require human sign-off"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (supply-op 50000) :confidence 0.99) st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest hard-on-op-not-allowlisted
  (testing "closed op-allowlist: no op outside the four coordination ops exists for this actor"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (log-op) :op :execute-concrete-pour) st)]
      (is (:hard? v))
      (is (some #(= :op-not-allowlisted (:rule %)) (:violations v))))))

(deftest hard-on-unknown-op
  (let [st (fresh-store)
        v (governor/check req {} (assoc (log-op) :op :unknown) st)]
    (is (:hard? v))
    (is (some #(= :op-not-allowlisted (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (log-op) :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest hard-on-scope-exclusion-finalize-pour
  (testing "a proposal to directly finalize a concrete-pour decision is a hard, permanent block"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (crew-op) :confidence 0.99
                                          :task "proceed with the concrete pour now") st)]
      (is (:hard? v))
      (is (some #(= :scope-exclusion (:rule %)) (:violations v))))))

(deftest hard-on-scope-exclusion-override-safety-officer
  (testing "a proposal to override the site safety officer's judgment is a hard, permanent block"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (flag-op) :confidence 0.99
                                          :concern "override the site safety officer's judgment and proceed") st)]
      (is (:hard? v))
      (is (some #(= :scope-exclusion (:rule %)) (:violations v))))))

(deftest scope-exclusion-not-overridable-by-high-confidence-or-allowlisted-op
  (testing "hard blocks are never overridable, even at max confidence on an allowlisted op"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (supply-op 100) :confidence 1.0
                                          :task "finalize the concrete pour") st)]
      (is (:hard? v))
      (is (not (:ok? v)))
      (is (not (:escalate? v))))))

(deftest hard-on-unregistered-client
  (let [st (fresh-store)
        v (governor/check {:client-id "nobody"} {} (log-op) st)]
    (is (:hard? v))
    (is (some #(= :no-client (:rule %)) (:violations v)))))

(deftest hard-on-unknown-site
  (let [st (fresh-store)
        v (governor/check req {} (assoc (log-op) :site-id "S-ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-site (:rule %)) (:violations v)))))

(deftest hard-on-foreign-site
  (let [st (fresh-store)]
    (store/register-client! st {:client-id "client-2" :name "Other"})
    (let [v (governor/check {:client-id "client-2"} {} (log-op) st)]
      (is (:hard? v))
      (is (some #(= :site-wrong-client (:rule %)) (:violations v))))))

(deftest hard-on-site-not-verified
  (testing "a site record merely existing is not enough — it must be independently verified"
    (let [st (fresh-store)]
      (store/register-site! st {:site-id "S-2" :client-id "client-1"
                                 :name "unverified-site" :verified? false
                                 :max-supply-cost 5000})
      (let [v (governor/check req {} (assoc (log-op) :site-id "S-2") st)]
        (is (:hard? v))
        (is (some #(= :site-not-verified (:rule %)) (:violations v)))))))

(deftest hard-on-unknown-worker-for-crew-operation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (crew-op) :worker-id "W-ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-worker (:rule %)) (:violations v)))))

(deftest hard-on-foreign-worker
  (let [st (fresh-store)]
    (store/register-client! st {:client-id "client-2" :name "Other"})
    (store/register-worker! st {:worker-id "W-2" :client-id "client-2"
                                :name "other-worker" :verified? true})
    (let [v (governor/check req {} (assoc (crew-op) :worker-id "W-2") st)]
      (is (:hard? v))
      (is (some #(= :worker-wrong-client (:rule %)) (:violations v))))))

(deftest hard-on-worker-not-verified
  (testing "a worker record merely existing is not enough — it must be independently verified"
    (let [st (fresh-store)]
      (store/register-worker! st {:worker-id "W-3" :client-id "client-1"
                                  :name "unverified-worker" :verified? false})
      (let [v (governor/check req {} (assoc (crew-op) :worker-id "W-3") st)]
        (is (:hard? v))
        (is (some #(= :worker-not-verified (:rule %)) (:violations v)))))))

(deftest always-escalates-flag-safety-concern-even-at-high-confidence
  (testing "a surfaced safety concern always goes to a human — the governor never resolves it itself"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (flag-op) :confidence 0.99) st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (log-op) :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))

;; Dedicated regression test for the self-tripping bug pattern: the
;; governor's scope-exclusion phrase list must never match inside the
;; mock advisor's own default rationale text for any allowlisted op.
;; Phrasing scope-exclusion terms as full finalization/execution
;; ACTION phrases (not bare nouns like "pour"/"safety"/"concrete")
;; is what keeps this actor's own vocabulary — pour-timing scheduling,
;; safety-concern flagging, concrete supply orders — from
;; self-blocking. See the namespace docstring on
;; `concretecrew.governor` for the design rationale.
(deftest default-mock-advisor-proposals-never-self-trip-scope-exclusion
  (let [st (fresh-store)
        mock (advisor/mock-advisor)
        requests [{:client-id "client-1" :op :log-work-record :stake :low
                   :site-id "S-1" :task "pour-timing update for footing pour"
                   :materials-usage {"aggregate" 12} :progress "50% of pour complete"}
                  {:client-id "client-1" :op :schedule-crew-operation :stake :low
                   :site-id "S-1" :worker-id "W-1" :task "schedule pour crew"
                   :scheduled-time "2026-07-20T08:00:00Z"}
                  {:client-id "client-1" :op :flag-safety-concern :stake :low
                   :site-id "S-1" :concern "site safety inspection pending before pour"}
                  {:client-id "client-1" :op :coordinate-supply-order :stake :low
                   :site-id "S-1" :material "ready-mix concrete" :quantity 8 :cost 2000}]]
    (doseq [request requests]
      (let [proposal (advisor/-advise mock st request)
            v (governor/check request {} proposal st)]
        (testing (str "op " (:op request) " never self-trips scope-exclusion")
          (is (not (some #(= :scope-exclusion (:rule %)) (:violations v)))
              (str "proposal rationale unexpectedly self-tripped: " (pr-str proposal))))))))
