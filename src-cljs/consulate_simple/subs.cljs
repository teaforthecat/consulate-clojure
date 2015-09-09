(ns consulate-simple.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [register-sub]]))

(register-sub
 :navigation
 (fn [db]
   (reaction (:navigation @db))))

(register-sub
 :datacenters
 (fn [db]
   (reaction (:datacenters @db))))
