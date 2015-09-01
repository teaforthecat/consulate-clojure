(ns consulate-simple.core-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]))

(deftest test-numbers
  (is (= 1 1)))


(deftest somewhat-less-wat
  (is (= "{}[]" (+ {} []))))

(deftest javascript-allows-div0
  (is (= js/Infinity (/ 1 0) (/ (int 1) (int 0)))))
