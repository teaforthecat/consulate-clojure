(ns consulate-simple.handlers
  (:require [consulate-simple.consul :as consul]
            [consulate-simple.db :as db]
            [reagent.session :as session]
            [re-frame.core :refer [register-handler debug path]]))

;; inspect the state with
;; re-frame.db/app-db

(register-handler
 :initialize-db
 (fn [app-db _]
   (consul/get-datacenters) ;; async
   (merge db/default-value app-db)))

(register-handler
 :datacenters-arrived
 [(path :datacenters) ]
 (fn [app-db [_ response]]
   (map consul/datacenter response)))


(register-handler
 :navigate
 [(path :navigation)]
 (fn [app-db [_ page query-params]]
   {:page page :args query-params}))


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
