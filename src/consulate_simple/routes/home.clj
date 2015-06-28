(ns consulate-simple.routes.home
  (:require [consulate-simple.layout :as layout]
            [compojure.core :refer [defroutes GET PUT routes make-route context]]
            [ring.util.http-response :refer [ok]]
            [consulate-simple.consul :as consul]
            [clojure.java.io :as io]
            [byte-streams :as bs]
            [taoensso.timbre :as timbre]))

(defn home-page []
  (layout/render "home.html"))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/docs" [] (ok (-> "docs/docs.md" io/resource slurp))))


(defn coerce-body [req]
  "special http-kit treatment - bytes"
  (let [body (:body req)]
    (bs/convert body String)))

(defroutes api-routes
  (context "/api" []
    (context "/kv" []
      (GET "/:key" [key] (ok (consul/get-kv key))) ;; get-kv
      (PUT "/:key" [key] (fn [req]
                           (let [value (coerce-body req)]
                             ;; stored as a string
                             (consul/put-kv key value)
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
