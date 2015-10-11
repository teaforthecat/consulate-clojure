(ns consulate-simple.consul
  (:require
    [clojure.string :refer (blank?)]
    [taoensso.timbre :as timbre]
    [cheshire.core :refer (generate-string parse-string)]
    [ring.util.codec :as codec]
    [org.httpkit.client :as client]
    [environ.core :refer [env]]
    [byte-streams :as bs]
    [clojurewerkz.route-one.core :refer [with-base-url url-for]]
    )
  (:import clojure.lang.ExceptionInfo))

(timbre/refer-timbre)

(defn call
   "remote http call"
   [verb url & [body]]
   (let [request-body (if body {:body (if (string? body) body (generate-string body))})]
     (let [{:keys [status body] :as res} @(verb url request-body)]
       (if-not (and status (> status 199) (< status 299))
         (throw (ExceptionInfo. (str "return code was " status ", not 200 something, using: " verb " result: " ) res))
         (parse-string body (fn [k] (keyword (.toLowerCase k))))
         ))))

(def consul-host-base-uri (env :consul-host-base-uri))
(def consul-resources
  {:kv "/v1/kv/:key"
   :catalog {
             :datacenters "/v1/catalog/datacenters"
             :nodes "/v1/catalog/nodes"
             :services "/v1/catalog/services"
             :service {
                       :nodes "/v1/catalog/service/:service"
                       }
             :node {
                    :services "/v1/catalog/node/:node"
                    }
             }
   :health {
            :node "/v1/health/node/:node"
            :checks "/v1/health/checks/:service"
            :service "/v1/health/service/:service"
            :state "/v1/health/state/:state"}
   :event {
           :fire "/v1/event/fire/:event"
           :list "/v1/event/list"}
   :status {
            :leader "/v1/status/leader"
            :peers "/v1/status/peers"}
   :agent "not interesting"
   :sessions "not interesting"
   :acl "idk"})


(defn url [ resource & [args :or {}] ]
  (with-base-url consul-host-base-uri
    (url-for (get-in consul-resources resource) args)))

(defn decode [s]
  (if (blank? s)
    s
    (String. (codec/base64-decode s))))


;; kv

(defn get-kv
  "get the value of a key or keys"
  [key]
  (let [response (call client/get (url [:kv] {:key key}))]
    (decode (:value (first response)))))

(defn get-kv-list
  "get the value of a key or keys"
  [key]
  (let [response (call client/get (url [:kv] {:key key :recurse true}))]
    (map (fn [{:keys [key value]}]
           {key (decode value)})  response)))

(defn delete-kv
  "delete the value of a key or keys"
  [key]
  (let [response (call client/delete (url [:kv] {:key key}))]
    response))

(defn delete-kv-list
  "delete the value of a key or keys"
  [key]
  (let [response (call client/delete (url [:kv] {:key key :recurse true}))]
    response))

(defn get-kv-keys
  "get the value of a key or keys"
  [key]
  (let [response (call client/get (url [:kv] {:key key :recurse true :keys true}))]
    response))

(defn put-kv
  "get the value of a key or keys; returns true or false"
  [key value]
  (call client/put (url [:kv] {:key key}) value ))


;; catalog
(defn get-datacenters []
  (call client/get (url [:catalog :datacenters])))

(defn get-nodes [& service]
  (if service ;;TODO support multiple services
    (call client/get (url [:catalog :service :nodes] {:service (first service)}))
    (call client/get (url [:catalog :nodes]))))

(defn get-services [& node]
  (if node
    (call client/get (url [:catalog :node :services] {:node node}))
    (call client/get (url [:catalog :services]))))


;; health
(defn get-node-health [node]
  (call client/get (url [:health :node] {:node node})))

(defn get-checks [service]
  (call client/get (url [:health :checks] {:service service})))

(defn get-service-health [service]
  (call client/get (url [:health :service] {:service service})))

(defn get-checks-for-state [state] ;; states: any, unknown, passing, warning, or critical
  (call client/get (url [:health :state] {:state state})))


;; events
(defn put-event [event body & [filters :or {}]]
  (call client/put (url [:event :fire] (merge {:event event} filters)) body))

(defn get-events [& event] ;;up to 256 events
  (let [response (call client/get (url [:event :list] (if event {:name event})))]
    (map #(update-in % [:payload] decode) response)))


;; status
(defn get-status []
  {:leader (call client/get (url [:status :leader]))
   :peers  (call client/get (url [:status :peers]))})
