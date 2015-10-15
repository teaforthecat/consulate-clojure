(ns consulate-simple.handlers-test
  (:require-macros [schema.core :as s])
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [consulate-simple.handlers :as handlers]
            [consulate-simple.schemas :as schemas]
            [schema.core :as s]
            [re-frame.core :refer [dispatch-sync]]))

;; #consulate-simple.consul.Datacenter{:name "dc1", :op_state "active", :status nil, :health_status "Healthy", :parents (#consulate-simple.consul.Parent{:id :dc1/this, :title :dc1/this, :link "#", :key :dc1/this, :value "\"that\""} #consulate-simple.consul.Parent{:id :dc1/x, :title :dc1/x, :link "#", :key :dc1/x, :value "\"y\""} #consulate-simple.consul.Parent{:id :dc1/y/a/b, :title :dc1/y/a/b, :link "#", :key :dc1/y/a/b, :value "\"c\""} #consulate-simple.consul.Parent{:id :dc1/z, :title :dc1/z, :link "#", :key :dc1/z, :value "\"zzz\""})}
;; #[{"dc1/this":"\"that\""},{"dc1/x":"\"y\""},{"dc1/y/a/b":"\"c\""},{"dc1/z":"\"zzz\""}]


;; (deftest events-response-triggers-something
;;   (let [response {:status 200 :body [{:x "y"}]}]
;;     ))

(def validates-event
  (let [thing {:name "hello"
               :payload "hello"
               :node-filter "hello"
               :service-filter "hello"
               :tag-filter "hello"}]
    (is (= thing (s/validate schemas/Event thing)))))

(def default-app-db
  {:datacenters
   [{:name "dc1" :op_state "active" :status nil :health_status "Healthy"}]})

(def mock-response-body
  [{:dc1/this "that"}
   {:dc1/x "y"}])

(def expected-parents
  (list {:id :dc1/this, :title :dc1/this, :link "#", :key :dc1/this, :value "that"}
        {:id :dc1/x, :title :dc1/x, :link "#", :key :dc1/x, :value "y"}))

(deftest clean-response
  (is (= (list
          {:id :dc1/this, :title :dc1/this, :link "#", :key :dc1/this, :value "that"}
          {:id :dc1/x, :title :dc1/x, :link "#", :key :dc1/x, :value "y"})
         (handlers/clean-response mock-response-body))))

(deftest delete-parent
  (is (= [{:id :dc1/x, :title :dc1/x, :link "#", :key :dc1/x, :value "y"}]
         (handlers/handle-delete-kv-response expected-parents [:handle-delete-kv-response "dc1/this" false]))))

(deftest get-index
  (let [app-db default-app-db
        name "dc1"]
    (is (= 0
           (handlers/get-index default-app-db name)))))

(deftest test-numbers
  (let [app-db default-app-db
        name "dc1"
        response {:success true :body mock-response-body}]
    (let [return-data (handlers/handle-kv-data-response app-db [:handle-kv-data-response name response])]
      (is (= :dc1/this
             (get-in return-data [:detail :parents 0 :id]))))))
