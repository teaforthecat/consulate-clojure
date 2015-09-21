(ns consulate-simple.routes.home
  (:require [consulate-simple.layout :as layout]
            [compojure.core :refer [defroutes GET PUT DELETE routes make-route context]]
            [ring.util.http-response :refer [ok not-found bad-gateway]]
            [consulate-simple.consul :as consul]
            [clojure.java.io :as io]
            [byte-streams :as bs]
            [taoensso.timbre :as timbre])
  (:import clojure.lang.ExceptionInfo))

(defn home-page []
  (layout/render "home.html"))

(defn consul-page []
  (layout/render "consul.html"))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/consul" [] (consul-page))
  (GET "/docs" [] (ok (-> "docs/docs.md" io/resource slurp))))


(defn coerce-body [req]
  "special http-kit treatment - bytes"
  (let [body (:body req)]
    (if (string? body)
      body
      (bs/convert body String))))
                                                          ;; (let [result (cond
                                                          ;;                recurse
                                                          ;;                (consul/get-kv-list key)
                                                          ;;                keys
                                                          ;;                (consul/get-kv-keys key)
                                                          ;;                :else
                                                          ;;                (consul/get-kv key))]

(defn make-key [scope item glob]
  (clojure.string/join "/" (filter string? [scope item glob])))

(defroutes api-routes
  (context "/api" []
    (context "/kv" []
      (GET ["/*"] [scope recurse keys] (fn [req] (try
                                                   (let [consul-key (get-in req [:route-params :*])
                                                         result (cond
                                                                  recurse
                                                                  (consul/get-kv-list consul-key)
                                                                  keys
                                                                  (consul/get-kv-keys consul-key)
                                                                  :else
                                                                  (consul/get-kv consul-key))]
                                                     (ok result))
                                                   (catch ExceptionInfo e ;; proxy error handling
                                                     (let [consul-response (.getData e)]
                                                       (if (= 404 (:status consul-response))
                                                         (let [message (str "key not found: " (get-in req [:route-params :*]))]
                                                           (timbre/info message)
                                                           (not-found message))
                                                         (let [message (str "received: " (:status consul-response) " " (:body consul-response))]
                                                           (timbre/error message)
                                                           (bad-gateway message)))))))) ;; get-kv
      (DELETE ["/*"] [scope recurse keys] (fn [req] (try
                                                   (let [consul-key (get-in req [:route-params :*])
                                                         result (cond
                                                                  recurse
                                                                  (consul/delete-kv-list consul-key)
                                                                  :else
                                                                  (consul/delete-kv consul-key))]
                                                     (timbre/info (str "deleting key: " consul-key))
                                                     (ok result))
                                                   (catch ExceptionInfo e ;; proxy error handling
                                                     (let [consul-response (.getData e)]
                                                       (if (= 404 (:status consul-response))
                                                         (let [message (str "key not found: " (get-in req [:route-params :*]))]
                                                           (timbre/info message)
                                                           (not-found message))
                                                         (let [message (str "received: " (:status consul-response) " " (:body consul-response))]
                                                           (timbre/error message)
                                                           (bad-gateway message)))))))) ;; get-kv
      (PUT ["/*"] [scope] (fn [req]
                            (let [consul-key (get-in req [:route-params :*])
                                  value (coerce-body req)]
                             ;; stored as a string
                             (consul/put-kv consul-key value)
                             (ok "success\n"))))) ;; put-kv
    (context "/catalog" []
      (GET "/datacenters" [] (ok (consul/get-datacenters)))  ; get-datacenters
      (GET "/nodes" [] (ok (consul/get-nodes ))) ;; get-nodes
      (GET "/services" [] (ok (consul/get-services ))) ;; get-services
      (GET "/service/:service" [service] (ok (consul/get-nodes service))) ;; get-service-nodes
      (GET "/node/:node" [node] (ok (consul/get-services node)))) ;; get-node-services
    (context "/health" []
      (GET "/node/:node" [node] (ok (consul/get-node-health node))) ;; get-nodes-health
      (GET "/checks/:service" [service] (ok (consul/get-checks service))) ;; get-checks
      (GET "/service/:service" [service] (ok (consul/get-service-health service)))  ;; get-service-health
      (GET "/state/:state" [state] (ok (consul/get-checks-for-state state))))  ;; get-checks-for-state
    (context "/event" []
      (GET "/list" [name] (ok (consul/get-events name)))
      (PUT "/fire/:event" [event]
        (fn [req]
          (timbre/info req)
          (let [value (coerce-body req)]
            (ok (consul/put-event event
                                  value
                                  (:params req)))))))))
