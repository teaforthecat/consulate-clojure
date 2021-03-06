(ns consulate-simple.handlers
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [schema.core :as s])
  (:require [consulate-simple.consul :as consul]
            [consulate-simple.db :as db]
            [consulate-simple.schemas :as schemas]
            [reagent.session :as session]
            [cljs.core.async :refer [<! take!]]
            [re-frame.core :refer [register-handler debug path dispatch dispatch-sync]]
            [schema.core :as s]))

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
   (merge db/default-db app-db)))

(defn submit-event-form [old-event-form [_ new-event-form]]
  (if new-event-form
    (let [event-form (merge old-event-form new-event-form)
          ; TODO: coerce or validate event-form
          ]
      (go
        (let [response (<! (consul/put-event event-form))]
          (if (:success response)
            (do
              (dispatch [:flash :success (str "Fired Event")])
              (dispatch [:handle-event-response response]))
            (dispatch [:flash :error (str "Error Firing Event")]))          ))
      (merge event-form {:active false}))
    db/default-event-form))

(defn handle-event-response [app-db [_ response]]
  (-> app-db
      (merge {:event-form db/default-event-form})
      (update-in [:detail :children] conj (:body response))))

(register-handler
 :handle-event-response
 [debug]
 handle-event-response)

(register-handler
 :submit-event-form
 [debug (path :event-form)]
 submit-event-form)

(register-handler
 :show-event-form
 [debug (path :event-form)]
 (fn [event-form [_]]
   (update event-form :active (constantly true))))

(register-handler
 :datacenters
 [(path :datacenters) ]
 (fn [app-db [_ response]]
   (vec (map consul/datacenter response))))

(defn handle-events-response [detail [_ response]]
  (let [events (:body response)]
    (update detail :children (constantly events))))

(register-handler
 :handle-events-response
 [debug (path :detail)]
 handle-events-response)

(defn detail-hook []
  (go
    (let [response (<! (consul/get-events))]
      (if (:success response)
        (dispatch [:handle-events-response response])
        (prn response)))))

(defn run-nav-hooks [app-db [_ page]]
  ((get {:detail detail-hook} page))
  nil);async only

(register-handler
 :run-nav-hooks
 []
 run-nav-hooks)

(register-handler
 :navigate
 [(path :navigation)]
 (fn [app-db [_ page query-params]]
   (dispatch [:run-nav-hooks page])
   {:page page :args query-params}))

(defn toggle-display-child [children [_ id]]
  (let [pos (position #(= id (:id %)) children)]
    (update-in children [pos :display] not)))

;(toggle-display-child [{:id "x"} {:id "y"}] [:x  "x"])

(register-handler
 :toggle-display-child
 [debug (path :detail :children)]
 toggle-display-child)

(def default-form
  {:active false
   :new-service-form
   {:consul-key nil
    :consul-value nil}})


(register-handler
 :init-new-service-form
 [ (path :new-service-form)]
 (fn [app-db [_]]
   default-form)) ;; default is to not show the form

(defn delete-kv [app-db [_ consul-key recurse]]
  (go
    (let [response (<! (consul/delete-kv consul-key))]
      (if (:success response)
        (do
          (dispatch [:flash :success (str "Deleted Key")])
           ;; get all instead of try to remove one - lazy
          (dispatch [:handle-delete-kv-response consul-key recurse]))
        (dispatch [:flash :error (str "Error Deleting Key")]))))
  ;; state changes later
  app-db)

(defn handle-delete-kv-response [parents [_ consul-key recurse]]
  (vec (filter #(not (= (keyword consul-key) (:title %))) parents)))

(register-handler
 :handle-delete-kv-response
 [(path [:detail :parents])]
 handle-delete-kv-response)

(register-handler
 :delete-kv
 []
 delete-kv)

(register-handler
 :show-new-service-form
 [ (path :new-service-form)]
 (fn [form [_ active]]
   (if active
     (merge form {:active true})
     default-form)))

(register-handler
 :flash
 [ (path :flash)]
 (fn [flash [_ state message]]
   (if (= :hide state)
     {:state :hide}
     (do
       (js/setTimeout #(dispatch-sync [:flash :hide]) 2000)
       {:state state
        :message message}))))

(register-handler
 :submit-new-service-form
 [ (path :new-service-form)]
 (fn [old-form [_ new-form]]
   (let [form (merge old-form new-form)
         key   (get-in form [:new-service-form :consul-key])
         value (get-in form [:new-service-form :consul-value])
         request (consul/put-kv key value)]
     (take! request (fn [response] (dispatch [:handle-kv-response response key value])))
     form)))

(register-handler
 :add-to-list
 [(path [:detail :parents])]
 (fn [parents [_ form]]
   (let [consul-key  (keyword (get-in form [:new-service-form :consul-key]))
         value (get-in form [:new-service-form :consul-value])
         parent (consul/map->Parent {:id consul-key :title consul-key :link "#" :key consul-key :value value})]
     (conj parents parent))))

(register-handler
 :handle-kv-response
 [ (path :new-service-form)]
 (fn [form [_ response]]
   (if (:success response)
     (do
       (dispatch [:flash :success (str "Saved Key")])
       (dispatch [:add-to-list form])
       default-form) ;; go back to initialized state
     (assoc-in form [:new-service-form :errors] (:body response)))))

(register-handler
 :get-kv-data
 []
 (fn [app-db [_ name]]
   (let [request (consul/get-kv name :recurse true)]
     (take! request (fn [response]
                      (dispatch [:handle-kv-data-response name response]))))
   app-db)) ;;maybe this is superfluous?

(defn clean-response [body]
  (map (fn [kv] (let [[k v] (first (vec kv))] ;first because there is only one
                  {:id k :title k :link "#" :key k :value v}))
       body))

(defn get-index [app-db name]
  (position #(= name (:name %)) (:datacenters app-db)))

(defn handle-kv-data-response [app-db [_ name response]]
  (if (:success response)
    (let [dc-index (get-index app-db name)
          body (:body response)
          cleaned-response (clean-response body)
          parents (map consul/map->Parent cleaned-response)]
      (-> app-db
          (update-in [:datacenters] #(vec %))  ;turn lazy-seq into vector
          (assoc-in [:datacenters dc-index :parents] (vec parents))
          ((fn [db]
             (assoc-in db [:detail] (get-in db [:datacenters dc-index]))))))
    app-db ;;leave untouched
  ))


(register-handler
 :handle-kv-data-response
 []
 handle-kv-data-response)

;; (register-handler
;;  :handle-kv-data-response
;; ; [debug (path :datacenters)]
;;  (fn [app-db [_ name response]]
;;    (if (:success response)
;;      (let [dc-index (position #(= name (:name %)) (:datacenters datacenters))
;;            body (:body response)
;;            cleaned-response (map (fn [kv] (let [[k v] (first (vec kv))] ;first because there is only one
;;                                             {:id k :title k :link "#" :key k :value v}))
;;                                  body)
;;            parents (map consul/map->Parent cleaned-response)]
;;        (update-in app-db [:datacenters] #(vec %)) ;turn lazy-seq into vector
;;        (assoc-in app-db [:datacenters dc-index :parents] parents)
;;        (accoc-in app-db [:detail] (get (:datacenters app-db) dc-index)))
;;        app-db ;;leave untouched
;;        )))


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
