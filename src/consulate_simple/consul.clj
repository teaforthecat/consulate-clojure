(ns consulate-simple.consul
  (:require
    [taoensso.timbre :as timbre]
    [cheshire.core :refer (generate-string parse-string)]
    [ring.util.codec :as codec]
    [org.httpkit.client :as client]
    [environ.core :refer [env]]
    [clojurewerkz.route-one.core :refer [with-base-url url-for]]
    )
  (:import clojure.lang.ExceptionInfo))

;; see conjul

(timbre/refer-timbre)

(defn call
   "remote http call"
   [verb url & [body]]
   (let [request-body (if body {:body (if (string? body) body (generate-string body))})]
     (let [{:keys [status body] :as res} @(verb url request-body)]
       (if-not (= status 200)
         (throw (ExceptionInfo. (str "failed to call" verb) res))
         (parse-string body (fn [k] (keyword (.toLowerCase k))))
         ))))

(def consul-host-base-uri (env :consul-host-base-uri))
(def consul-resources {:kv "/v1/kv/:key"})


(defn url [ resource args & query]
  (with-base-url consul-host-base-uri
    (url-for (resource consul-resources) args)))

(defn decode [s]
  (String. (codec/base64-decode s)))

(defn get-kv
  "get the value of a key or keys"
  [key]
  (let [response (call client/get (url :kv {:key key}))]
    (decode (:value (first response)))))

(defn get-kv-list
  "get the value of a key or keys"
  [key]
  (let [response (call client/get (url :kv {:key key :recurse true}))]
    (map (fn [{:keys [key value]}]
           {key (decode value)})  response)))

(defn get-kv-keys
  "get the value of a key or keys"
  [key]
  (let [response (call client/get (url :kv {:key key :recurse true :keys true}))]
    response))

(defn put-kv
  "get the value of a key or keys"
  [key value]
  (call client/put (url :kv {:key key}) value ))
