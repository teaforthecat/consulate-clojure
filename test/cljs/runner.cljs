(ns consulate-simple.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [consulate-simple.core-test]
              ;[consulate-simple.util-test]
              ))

(doo-tests 'consulate-simple.core-test
           ;'consulate-simple.util-test
           )
