(ns consulate-simple.db
  (:require-macros [schema.core :as s])
  (:require [consulate-simple.schemas :as schemas]
            [schema.core :as s]))

(def default-event-form
  " a valid Event to populate a form"
  (s/validate schemas/Event {:name ""
                             :payload ""
                             :node-filter ""
                             :service-filter ""
                             :tag-filter ""
                             :active false}))

(def default-db
  {:datacenters []
   :history []
   :event-form default-event-form} )
