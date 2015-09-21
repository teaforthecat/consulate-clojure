(ns consulate-simple.consul
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [consulate-simple.config :refer (config)]
    ;; [consulate-simple.routes :refer (path-for)]
    ;; [com.rpl.specter :as s]
    ;; [consulate-simple.db :as db]
    [re-frame.core :refer [subscribe dispatch]]
    [cljs-http.client :as http]
    [cljs.core.async :refer [<! take!]]
    [ajax.core :refer [GET PUT POST]]))

(def datacenters-path (str (:root-path config) "api/catalog/datacenters"))
(def kv-path (str (:root-path config) "api/kv"))
(def nodes-path (str (:root-path config) "api/catalog/nodes"))
(def services-path (str (:root-path config) "api/catalog/services"))
(def service-nodes-path (str (:root-path config) "api/catalog/service/"))

(defrecord Datacenter [name op_state status])
(defrecord Parent [id title link key value])

(defn datacenter [name]
  (map->Datacenter {:name name :op_state "active" :health_status "Healthy"}))

(defn get-datacenters []
  "coerces list of datacenter names from the server into Datacenter records, and updates APP-STATE"
  (GET datacenters-path
      :handler #(dispatch [:datacenters %])
      ))

(defn get-datacenters-async []
  (http/get datacenters-path))

(defn get-datacenter-detail [name app-state]
  (if-not (:datacenters app-state) (get-datacenters))
  (swap! app-state assoc-in [:detail] (datacenter name)))

(defn put-kv [key value]
  (http/put (str kv-path "/" key) {:edn-params value}))

(defn get-kv [key & options]
  (let [moptions (apply hash-map options)]
    (http/get (str kv-path "/" key) {:query-params moptions})))

(defn delete-kv [key & options]
  (let [moptions (apply hash-map options)]
    (http/delete (str kv-path "/" key) {:query-params moptions})))

(defn get-services []
  (http/get services-path))

(defn get-service-nodes [service-name]
  (http/get (str service-nodes-path service-name)))

(defn get-nodes []
  (http/get (str nodes-path)))

;; (defn get-datacenter-detail [name app-state]
;;   (let [q [:datacenters s/ALL #(= name (:name %))]
;;         datacenter (first (s/select q @app-state))]
;;     (swap! app-state assoc-in [:detail] datacenter)))


(defn get-detail [app-state]
  (get-datacenters))


(defn initialize-data [app-state]
  (get-datacenters))
