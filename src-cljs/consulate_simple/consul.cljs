(ns consulate-simple.consul
  (:require [consulate-simple.config :refer (config)]
            ;; [consulate-simple.routes :refer (path-for)]
            ;; [com.rpl.specter :as s]
            ;; [consulate-simple.db :as db]
            [ajax.core :refer [GET PUT POST]]))

(def datacenters-path (str (:root-path config) "api/catalog/datacenters"))

(defrecord Datacenter [name op_state status])

(defn datacenter [name]
  (map->Datacenter {:name name :op_state "active" :health_status "Healthy"}))

(defn get-datacenters [app-state]
  "coerces list of datacenter names from the server into Datacenter records, and updates APP-STATE"
  (GET datacenters-path
      :handler #(swap! app-state update-in [:datacenters] (fn [] (map datacenter %)))
      ))

(defn get-datacenter-detail [name app-state]
  (if-not (:datacenters app-state) (get-datacenters app-state))
  (swap! app-state assoc-in [:detail] (datacenter name)))


;; (defn get-datacenter-detail [name app-state]
;;   (let [q [:datacenters s/ALL #(= name (:name %))]
;;         datacenter (first (s/select q @app-state))]
;;     (swap! app-state assoc-in [:detail] datacenter)))


(defn get-detail [app-state]
  (get-datacenters app-state))


(defn initialize-data [app-state]
  (get-datacenters app-state))
