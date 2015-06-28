(ns consulate-simple.consul-test
  (:require [clojure.test :refer :all]
            [ring.util.codec :as codec]
            [clojure.string :as s]
            [cheshire.core :refer (generate-string parse-string)]
            [consulate-simple.consul :as consul]))

(defn test-get [url request-body]
  (let [dummy-promise (promise)
        data {:value (codec/base64-encode
                      (byte-array (map byte "world")) )}]
    (deliver dummy-promise {:status 200
                            :body (generate-string data)})))

(defn resolve-response [resource] ;;list of keywords
  (str "test/responses/" (s/join "_" (map name resource)) ".json"))

(defn test-get-file [& [:as resource]]
  (let [path (resolve-response resource)
        p (promise)]
    (deliver p {:status 200 :body (slurp path)})
    (fn [url request-body] p))) ;;signature of 'get; fake promise



(deftest consul-kv
  (with-redefs [org.httpkit.client/get (test-get-file :kv)]
    (is (= (consul/get-kv "anything" ) ;; get-kv
           "test")));; note: (decode "dGVzdA==") => "test"

  (with-redefs [org.httpkit.client/get (test-get-file :kv :keys)]
    (is (= (consul/get-kv-keys "anything" ) ;; get-kv-keys
           [ "/web/bar" "/web/foo" "/web/subdir/"])))

  (with-redefs [org.httpkit.client/get (test-get-file :kv)]
    (is (= (consul/get-kv-list "anything" ) ;; get-kv-list
           [{"zip" "test"}]))) ;; note: keys are strings

  )

(deftest consul-catalog

  (with-redefs [org.httpkit.client/get (test-get-file :catalog :datacenters)]
    (is (= (consul/get-datacenters) ;; get-datacenters
           ["dc1", "dc2"])))

  (with-redefs [org.httpkit.client/get (test-get-file :catalog :nodes)]
    (is (= (consul/get-nodes) ;; get-nodes
           [{:node "baz", :address "10.1.10.11"}
             {:node "foobar", :address "10.1.10.12"}]))) ; note keys are keywords

  (with-redefs [org.httpkit.client/get (test-get-file :catalog :services)]
    (is (= (consul/get-services) ;; get-services
           {:consul [], :redis [], :postgresql ["master" "slave"]}))) ;; note: values are tags

  (with-redefs [org.httpkit.client/get (test-get-file :catalog :service :nodes)]
    (is (= (consul/get-nodes "any-service-name") ;; get-nodes
           [{:node "foobar",
              :address "10.1.10.12",
              :serviceid "redis",
              :servicename "redis",
              :servicetags nil,
              :serviceaddress "",
              :serviceport 8000}])))

  (with-redefs [org.httpkit.client/get (test-get-file :catalog :node :services)]
    (is (= (consul/get-services "any-node-fqdn") ;; get-services
           {:node {:node "foobar", :address "10.1.10.12"},
             :services
             {:consul
              {:id "consul", :service "consul", :tags nil, :port 8300},
              :redis
              {:id "redis",
               :service "redis",
               :tags ["v1"],
               :port 8000}}})))

)
(deftest consul-health

  (with-redefs [org.httpkit.client/get (test-get-file :health :node)]
    (is (= (consul/get-node-health "any-node-fqdn") ;; get-node-health
           [{:node "foobar",
              :checkid "serfHealth",
              :name "Serf Health Status",
              :status "passing",
              :notes "",
              :output "",
              :serviceid "",
              :servicename ""}
             {:node "foobar",
              :checkid "service:redis",
              :name "Service 'redis' check",
              :status "passing",
              :notes "",
              :output "",
              :serviceid "redis",
              :servicename "redis"}])))

  (with-redefs [org.httpkit.client/get (test-get-file :health :checks)]
    (is (= (consul/get-checks "any-service") ;; get-checks
           [{:node "foobar",
              :checkid "service:redis",
              :name "Service 'redis' check",
              :status "passing",
              :notes "",
              :output "",
              :serviceid "redis",
             :servicename "redis"}])))

  (with-redefs [org.httpkit.client/get (test-get-file :health :node)]
    (is (= (consul/get-service-health "any-service") ;; get-service-health
           [{:node "foobar",
             :checkid "serfHealth",
             :name "Serf Health Status",
             :status "passing",
             :notes "",
             :output "",
             :serviceid "",
             :servicename ""}
            {:node "foobar",
             :checkid "service:redis",
             :name "Service 'redis' check",
             :status "passing",
             :notes "",
             :output "",
             :serviceid "redis",
             :servicename "redis"}])))


  (with-redefs [org.httpkit.client/get (test-get-file :health :state)]
    (is (= (consul/get-checks-for-state "any-state") ;; get-checks-for-state
           [{:node "foobar",
             :checkid "serfHealth",
             :name "Serf Health Status",
             :status "passing",
             :notes "",
             :output "",
             :serviceid "",
             :servicename ""}
            {:node "foobar",
             :checkid "service:redis",
             :name "Service 'redis' check",
             :status "passing",
             :notes "",
             :output "",
             :serviceid "redis",
             :servicename "redis"}])))
)

(deftest consul-events
    (with-redefs [org.httpkit.client/put (test-get-file :event :fire)]
    (is (= (consul/put-event "any-event" {:any "data"}) ;; put-event
           {:id "b54fe110-7af5-cafc-d1fb-afc8ba432b1c",
              :name "deploy",
              :payload nil,
              :nodefilter "",
              :servicefilter "",
              :tagfilter "",
              :version 1,
              :ltime 0}))) ;;note this is note a list

    (with-redefs [org.httpkit.client/get (test-get-file :event :list)]
      (is (= (consul/get-events "any-event") ;; get-events
             [{:id "b54fe110-7af5-cafc-d1fb-afc8ba432b1c",
              :name "deploy",
              :payload "1609030",
              :nodefilter "",
              :servicefilter "",
              :tagfilter "",
              :version 1,
              :ltime 19}])))

)

;; TODO: figure out how to test multiple requests in a "get-" function
;; (deftest consul-status
;;     (with-redefs [org.httpkit.client/get (test-get-file :status)]
;;       (is (= (consul/get-status) ;; get-status
;;              {:leader "10.1.10.12:8300"
;;               :status ["10.1.10.12:8300" "10.1.10.11:8300" "10.1.10.10:8300"] })))
;;   )
