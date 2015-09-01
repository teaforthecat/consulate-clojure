(ns  consulate-simple.handlers
  (:require [consulate-simple.consul :as consul]
            [consulate-simple.db :as db]
            [reagent.session :as session]
            [re-frame.core :refer [register-handler debug path]]))


(register-handler
 :initialise-db
 debug
 (fn [_ _]
   (consul/get-datacenters) ;; async
   db/default-value))

(register-handler
 :datacenters-arrived
 [ debug
  (path :datacenters) ]
 (fn [app-db [_ response]]
   (map consul/datacenter response)))


(register-handler
 :set-page
 (fn [app-db [_ name & args]]


   (session/put! :page :datacenters)))

;; -------------------------
;; Handlers

;; (defn consul-handler []
;;   ;; (consul/get-datacenters app-state)
;;   (dispatch-sync [:initialise-db])
;;   (session/put! :page :datacenters))

;; (defn datacenter-handler [name]
;;   (go
;;     ;; if we already have datacenters then use it, else fetch it
;;     ;; there is only one consul resource for all datacenters
;;     (let [dcs (or (get @app-state :datacenters)
;;                   (let [{:keys [status body]} (<! (consul/get-datacenters-async))]
;;                     (if (and (= 200 status) body)
;;                       (let [new-dcs (map consul/datacenter body)]
;;                         (update-app-state [:datacenters] new-dcs)
;;                         new-dcs)
;;                       (println "oh no! ..."))))]

;;       (if (some #(= name (:name %)) dcs) ;;"name" exists
;;         (do
;;           (update-app-state [:detail] (consul/datacenter name))
;;           (let [{status :status services :body} (<! (consul/get-services))]
;;             (if (and (= 200 status) services)
;;               (update-app-state [:detail :children] services)))
;;           (session/put! :page :detail))
;;         (session/put! :page :not-found)))))
