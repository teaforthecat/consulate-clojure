(ns consulate-simple.consul
  (:require [consulate-simple.config :refer (config)]
            ;; [consulate-simple.routes :refer (path-for)]
            [ajax.core :refer [GET PUT POST]]))



(defn get-datacenters [app-state]
  (GET (str (:root-path config) "api/catalog/datacenters.edn")
      :handler #(swap! app-state assoc-in [:datacenters] %)))
