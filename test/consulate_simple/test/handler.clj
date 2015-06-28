(ns consulate-simple.test.handler
  (:require [clojure.test :refer (deftest testing is)]
            [ring.mock.request :refer (request)]
            [consulate-simple.handler :refer (app)]))

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
          (is (= "success\n" (:body response)))))))
  ;; etc...
  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= 404 (:status response))))))
