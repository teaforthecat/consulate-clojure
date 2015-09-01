(ns consulate-simple.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [register-sub]]))


(register-sub
 :datacenters
 (fn [db]
   (reaction (:datacenters @db))))
