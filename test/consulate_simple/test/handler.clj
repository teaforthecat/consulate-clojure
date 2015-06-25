(ns consulate-simple.test.handler
  (:require [clojure.test :refer :all]
            [ring.mock.request :refer :all]
            [consulate-simple.handler :refer :all]))

(deftest test-app
  (testing "main route"
    (let [response (app (request :get "/"))]
      (is (= 200 (:status response)))))

  (testing "api route"
    (let [response (app (request :get "/api/kv/hello"))]
      (is (= 200 (:status response)))
      (is (= "world" (:body response)))
      ))

  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= 404 (:status response))))))
