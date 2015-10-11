(ns consulate-simple.schemas
    (:require-macros [cljs.core.async.macros :refer [go]]
                     [schema.core :as s])
    (:require [schema.core :as s]))


(def Event
  "a schema for a Consul Event"
  {:active s/Bool
   :name s/Str
   :payload s/Str ;;json maybe?
   :node-filter s/Str
   :service-filter s/Str
   :tag-filter s/Str
   })
