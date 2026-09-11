(ns concretecrew.advisor
  "Concrete Crew Coordination Advisor — the advisor named in this
  repository's README, proposing a job-site scheduling/logistics
  coordination operation (log a work record, schedule a crew
  operation, flag a safety concern, coordinate a supply order) from a
  site work order, crew roster and materials plan. Swappable
  mock/llm; the advisor ONLY proposes — `concretecrew.governor` checks
  site/worker provenance and independent verification, the closed
  op-allowlist and the concrete-pour/finishing-execution and
  safety-officer-override scope exclusion independently, and always
  escalates safety concerns and over-ceiling supply orders. This
  actor coordinates JOB-SITE SCHEDULING/LOGISTICS ONLY — it never
  performs concrete work itself and never proposes finalizing a
  concrete-pour/finishing-execution decision or overriding a
  site-safety officer's judgment. Modeled on
  cloud-itonami-isco-3313's accountingsupport.advisor and
  cloud-itonami-isco-7115's carpentry.advisor.

  A proposal: {:op :log-work-record|:schedule-crew-operation|:flag-safety-concern|:coordinate-supply-order
               :effect :propose :site-id str :worker-id str
               :task str :materials-usage map :progress str
               :scheduled-time str :concern str :material str
               :quantity number :cost number :stake kw
               :confidence n :rationale str}"
  )

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake site-id worker-id task materials-usage
                              progress scheduled-time concern material
                              quantity cost] :as request}]
  {:op op
   :effect :propose
   :site-id site-id
   :worker-id worker-id
   :task task
   :materials-usage materials-usage
   :progress progress
   :scheduled-time scheduled-time
   :concern concern
   :material material
   :quantity quantity
   :cost cost
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name op) " for site " site-id
                    " (client " (:client-id request) ")")})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a job-site scheduling and logistics coordination advisor
   for a concrete-placement crew. You coordinate scheduling and
   logistics ONLY — you never perform concrete work yourself, and you
   must NEVER propose finalizing a concrete-pour or
   finishing-execution decision, and NEVER propose overriding a site
   safety officer's judgment; those decisions belong exclusively to
   the humans on site. Given a request, propose one of
   :log-work-record, :schedule-crew-operation, :flag-safety-concern or
   :coordinate-supply-order, citing the registered :site-id (and
   :worker-id for crew operations), an honest :confidence and a
   :stake. Safety concerns always require human sign-off. Supply
   orders above the site's registered cost ceiling always require
   human sign-off regardless of confidence — the governor checks both
   independently.")

(defn- parse-proposal [content]
  (try
    (let [p (read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
