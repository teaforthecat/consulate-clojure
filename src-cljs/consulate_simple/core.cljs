(ns consulate-simple.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [cljs.core.async :refer [<! >! take! put!]]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [markdown.core :refer [md->html]]
            [consulate-simple.consul :as consul]
            [consulate-simple.partials :refer [navbar]]
            [consulate-simple.pages :refer [pages]]
            ;; [consulate-simple.db :as db]
            [ajax.core :refer [GET POST]])
  (:import goog.History))

(defonce app-state (reagent/atom
                    {:datacenters []
                     :history []} ))

(defn update-app-state [path value]
  (swap! app-state assoc-in path value))

(defn page []
  [(pages (or (session/get :page) :not-found)) app-state])


;; -------------------------
;; Handlers

(defn consul-handler []
  (consul/get-datacenters app-state)
  (session/put! :page :datacenters))

(defn datacenter-handler [name]
  (go
    ;; if we already have datacenters then use it, else fetch it
    ;; there is only one consul resource for all datacenters
    (let [dcs (or (get @app-state :datacenters)
                  (let [{:keys [status body]} (<! (consul/get-datacenters-async))]
                    (if (and (= 200 status) body)
                      (let [new-dcs (map consul/datacenter body)]
                        (update-app-state [:datacenters] new-dcs)
                        new-dcs)
                      (println "oh no! ..."))))]

      (if (some #(= name (:name %)) dcs) ;;"name" exists
        (do
          (update-app-state [:detail] (consul/datacenter name))
          (let [{status :status services :body} (<! (consul/get-services))]
            (if (and (= 200 status) services)
              (update-app-state [:detail :children] services)))
          (session/put! :page :detail))
        (session/put! :page :not-found)))))


;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :page :home))

(secretary/defroute "/about" []
  (session/put! :page :about))

(secretary/defroute "/consul" []
  (consul-handler))

(secretary/defroute "/consul/datacenters/:name" [name]
  (datacenter-handler name))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
        (events/listen
          EventType/NAVIGATE
          (fn [event]
              (secretary/dispatch! (.-token event))))
        (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn fetch-docs! []
      (GET "/docs" {:handler #(session/put! :docs %)}))

(defn mount-components []
  (reagent/render-component [#'navbar] (.getElementById js/document "navbar"))
  (reagent/render-component [#'page] (.getElementById js/document "app")))

(defn init! []
  (fetch-docs!)
  (consulate-simple.config/get-config)
  (hook-browser-navigation!)
  (secretary/dispatch! (.-hash js/window.location))
  (mount-components)
  ;; (db/link-state app-state)
  (consul/initialize-data app-state)
  )
