(ns consulate-simple.test.handler
  (:require [clojure.test :refer (deftest testing is)]
            [ring.mock.request :refer (request)]
            [consulate-simple.handler :refer (app)]
            [taoensso.timbre :as timbre])
  (:import clojure.lang.ExceptionInfo))

(deftest test-app
  (testing "main route"
    (let [response (app (request :get "/"))]
      (is (= 200 (:status response)))))

  (testing "api"
    (testing "kv"
      (with-redefs [consulate-simple.consul/get-kv (fn [key] "world" )
                    consulate-simple.consul/put-kv (fn [key value] true )]
        (let [response (app (request :get "/api/kv/hello"))]
          (is (= 200 (:status response)))
          (is (= "world" (:body response))))
        (let [response (app (request :put "/api/kv/hello" "world"))]
          (is (= 200 (:status response)))
          (is (= "success\n" (:body response)))))
      (testing "not-found response from consul"
        (with-redefs [consulate-simple.consul/get-kv (fn [key]
                                                       (throw (ExceptionInfo. (str "not found")
                                                                              {:status 404 :body "not found"})))
                      taoensso.timbre/info (fn [m])]
          (let [response (app (request :get "/api/kv/nothing"))]
            (is (= 404 (:status response))))))))
  ;; etc...
  (testing "not-found route"
    (with-redefs [taoensso.timbre/info (fn [m])]
      (let [response (app (request :get "/invalid"))]
        (is (= 404 (:status response)))))))
