(ns concretecrew.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [concretecrew.actor :as actor]
            [concretecrew.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Concrete Crew"})
    (store/register-site! st {:site-id "S-1" :client-id "client-1"
                               :name "site-042" :verified? true
                               :max-supply-cost 5000})
    (store/register-worker! st {:worker-id "W-1" :client-id "client-1"
                                :name "worker-042" :verified? true})
    st))

(deftest commits-a-verified-site-log-work-record
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :log-work-record :stake :low
                 :site-id "S-1" :task "footing pour prep"
                 :materials-usage {"aggregate" 12} :progress "50%"}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "client-1"))))))

(deftest holds-a-request-referencing-an-unverified-site
  (testing "a site record merely existing is not enough — hard block, never committed"
    (let [st (fresh-store)]
      (store/register-site! st {:site-id "S-2" :client-id "client-1"
                                :name "unverified-site" :verified? false
                                :max-supply-cost 5000})
      (let [graph (actor/build-graph {:store st})
            request {:client-id "client-1" :op :log-work-record :stake :low
                     :site-id "S-2" :task "footing pour prep"
                     :materials-usage {"aggregate" 12} :progress "50%"}
            result (actor/run-request! graph request {} "thread-2")]
        (is (= :hold (:disposition (:state result))))
        (is (empty? (store/records-of st "client-1")))))))

(deftest holds-a-request-with-scope-excluded-task
  (testing "a proposal to finalize a concrete pour is a hard, permanent block — never reaches request-approval"
    (let [st (fresh-store)
          graph (actor/build-graph {:store st})
          request {:client-id "client-1" :op :schedule-crew-operation :stake :low
                   :site-id "S-1" :worker-id "W-1"
                   :task "proceed with the concrete pour now"
                   :scheduled-time "2026-07-20T08:00:00Z"}
          result (actor/run-request! graph request {} "thread-3")]
      (is (= :hold (:disposition (:state result))))
      (is (empty? (store/records-of st "client-1"))))))

(deftest interrupts-then-approves-a-safety-concern-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :flag-safety-concern :stake :low
                 :site-id "S-1" :concern "wet weather delaying cure"}
        interrupted (actor/run-request! graph request {} "thread-4")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "client-1")))
    (let [resumed (actor/approve! graph "thread-4")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "client-1")))))))

(deftest interrupts-then-approves-an-over-ceiling-supply-order-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :coordinate-supply-order :stake :low
                 :site-id "S-1" :material "ready-mix concrete" :quantity 20 :cost 50000}
        interrupted (actor/run-request! graph request {} "thread-5")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "client-1")))
    (let [resumed (actor/approve! graph "thread-5")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "client-1")))))))
