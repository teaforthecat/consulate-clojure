(ns consulate-simple.core-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [consulate-simple.core]
            [re-frame.core :refer [dispatch-sync]]))

(deftest test-numbers
  (is (= 1 1)))

;; (deftest somewhat-less-wat
;;   (is (= "{}[]" (+ {} []))))

;; (deftest javascript-allows-div0
;;   (is (= js/Infinity (/ 1 0) (/ (int 1) (int 0)))))

;; (deftest can-navigate-to-consul
;;   (is (= null
;;          (dispatch-sync [:initialize-db]))))

;; (deftest can-navigate-to-datacenter-by-name
;;   (let [current_path (.-hash js/window.location)
;;         name "dc1"]
;;     (dispatch-sync [:navigate :detail {:name name}])
;;     (is (= current_path (.-hash js/window.location)))
;;     ;; (is (= current_path "wut"))
;;     )
;;  )

;; (deftest test-async
;;   (async done
;;     (http/get "http://foo.com/api.edn"
;;       (fn [res]
;;         (is (= res :awesome))
;;         (done)))))
