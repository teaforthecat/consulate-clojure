(ns consulate-simple.routes.home
  (:require [consulate-simple.layout :as layout]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :refer [ok]]
            [consulate-simple.consul :as consul]
            [clojure.java.io :as io]))

(defn home-page []
  (layout/render "home.html"))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/docs" [] (ok (-> "docs/docs.md" io/resource slurp))))


(defn get-kv [key]
  (consul/get-kv key))

(defroutes api-routes
  (GET "/api/kv/:key" [key] (get-kv key)))
