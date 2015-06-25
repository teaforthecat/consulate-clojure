(ns consulate-simple.consul
  (:require
    [taoensso.timbre :as timbre]
    [cheshire.core :refer (generate-string parse-string)]
    [ring.util.codec :as codec]
    [org.httpkit.client :as client])
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

(defn register
   "register a host in consule"
   [host name- ip dc]
   (call client/put (str host "/v1/catalog/register") {"Datacenter" dc "Node" name-  "Address" ip}))

(defn de-register
   "register a host in consule"
   [host name- dc]
   (call client/put (str host "/v1/catalog/deregister") {"Datacenter" dc "Node" name-}))

(defn decode [s]
  (String. (codec/base64-decode s)))

(defn get-kv
  "get the value of a key or keys"
  [host key]
  (let [response (call client/get (str host "/v1/kv/" key))]
    (decode (:value (first response)))))

(defn get-kv-list
  "get the value of a key or keys"
  [host key]
  (let [response (call client/get (str host "/v1/kv/" key "?recurse"))]
    (map (fn [{:keys [key value]}]
           {key (decode value)})  response)))

(defn get-kv-keys
  "get the value of a key or keys"
  [host key]
  (let [response (call client/get (str host "/v1/kv/" key
                                       "?recurse=true&keys=true"))]
    response))

(defn put-kv
  "get the value of a key or keys"
  [host key value]
  (call client/put (str host "/v1/kv/" key) value ))
