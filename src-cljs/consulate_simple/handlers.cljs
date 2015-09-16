(ns consulate-simple.handlers
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [consulate-simple.consul :as consul]
            [consulate-simple.db :as db]
            [reagent.session :as session]
            [cljs.core.async :refer [<! take!]]
            [re-frame.core :refer [register-handler debug path dispatch dispatch-sync]]))

;; inspect the state with
;; re-frame.db/app-db
(defn position [f coll & {:keys [from-end all] :or {from-end false all false}}]
  (let [all-idxs (keep-indexed (fn [idx val] (when (f val) idx)) coll)]
  (cond
   (true? from-end) (last all-idxs)
   (true? all)      all-idxs
   :else            (first all-idxs))))

(register-handler
 :initialize-db
 (fn [app-db _]
   (consul/get-datacenters) ;; async
   (merge db/default-value app-db)))

(register-handler
 :datacenters
 [(path :datacenters) ]
 (fn [app-db [_ response]]
   (vec (map consul/datacenter response))))

(register-handler
 :navigate
 [(path :navigation)]
 (fn [app-db [_ page query-params]]
   {:page page :args query-params}))

(def default-form
  {:active false
   :new-service-form
   {:consul-key nil
    :consul-value nil}})

(register-handler
 :init-new-service-form
 [debug (path :new-service-form)]
 (fn [app-db [_]]
   default-form)) ;; default is to not show the form

(register-handler
 :add-new-service-form
 [debug (path :new-service-form)]
 (fn [form [_]]
   (merge form {:active true})))

(register-handler
 :flash
 [debug (path :flash)]
 (fn [flash [_ state message]]
   (if (= :hide state)
     {:state :hide}
     (do
       (js/setTimeout #(dispatch-sync [:flash :hide]) 2000)
       {:state state
        :message message}))))

(register-handler
 :submit-new-service-form
 [debug (path :new-service-form)]
 (fn [old-form [_ new-form]]
   (let [form (merge old-form new-form)
         key   (get-in form [:new-service-form :consul-key])
         value (get-in form [:new-service-form :consul-value])
         request (consul/put-kv key value)]
     (take! request (fn [response] (dispatch [:handle-kv-response response key value])))
     form)))

(register-handler
 :add-to-list
 (fn [app-db [_ form]]
   (let [name (get-in app-db [:navigation :args :name])
         idx (position #(= name (:name %)) (:datacenters app-db))
         key   (get-in form [:new-service-form :consul-key])
         value (get-in form [:new-service-form :consul-value])
         parent (consul/map->Parent {:id key :title key :link "#" :key key :value value})]
     (update-in app-db [:datacenters idx :parents] conj parent))))

(register-handler
 :handle-kv-response
 [debug (path :new-service-form)]
 (fn [form [_ response]]
   (if (:success response)
     (do
       (dispatch [:flash :success (str "Saved Key")])
       (dispatch [:add-to-list form])
       default-form) ;; go back to initialized state
     (assoc-in form [:new-service-form :errors] (:body response)))))

(register-handler
 :get-kv-data
 [debug]
 (fn [app-db [_ name]]
   (let [request (consul/get-kv name :recurse true)]
     (take! request (fn [response]
                      (dispatch [:handle-kv-data-response name response]))))
   app-db)) ;;maybe this is superfluous?


(register-handler
 :handle-kv-data-response
 [debug (path :datacenters)]
 (fn [datacenters [_ name response]]
   (if (:success response)
     (let [dc-index (position #(= name (:name %)) datacenters)
           body (:body response)
           cleaned-response (map (fn [kv] (let [[k v] (first (vec kv))] ;first because there is only one
                                            {:id k :title k :link "#" :key k :value v}))
                                 body)
           parents (map consul/map->Parent cleaned-response)]
       (assoc-in (vec datacenters) [dc-index :parents] parents))
       datacenters ;;leave untouched
       )))


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
