(ns advertising-marketing.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [advertising-marketing.actor :as actor]
            [advertising-marketing.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-campaign! st {:campaign-id "campaign-1" :name "Spring Launch"})
    st))

(deftest commits-a-clean-low-risk-request
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:campaign-id "campaign-1" :op :produce :stake :low}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "campaign-1"))))))

(deftest holds-on-unregistered-campaign-without-committing
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:campaign-id "no-such-campaign" :op :produce :stake :low}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :done (:status result)))
    (is (nil? (get-in result [:state :record])))
    (is (empty? (store/records-of st "no-such-campaign")))
    (is (= :hold (:disposition (:state result))))))

(deftest interrupts-then-commits-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        ;; unsubstantiated-claim publication always escalates (governor invariant)
        request {:campaign-id "campaign-1" :op :publish-unsubstantiated-claim :stake :high}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "campaign-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (some? (get-in resumed [:state :record])))
      (is (= 1 (count (store/records-of st "campaign-1")))))))
