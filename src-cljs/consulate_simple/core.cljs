(ns consulate-simple.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [markdown.core :refer [md->html]]
            [consulate-simple.partials :refer [navbar]]
            [consulate-simple.pages :refer [pages]]
            [ajax.core :refer [GET POST]])
  (:import goog.History))

(defonce app-state (reagent/atom
                    {:datacenters []} ))

(defn update-app-state [path value]
  (swap! app-state assoc-in path value))

(defn page []
  [(pages (session/get :page))])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :page :home))

(secretary/defroute "/about" []
  (session/put! :page :about))

(secretary/defroute "/consul" []
  (consul/get-datacenters app-state)
  (session/put! :page :datacenters))


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
  (session/put! :page :home)
  (mount-components))
