(ns consulate-simple.core
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [cljs.core.async :refer [<! >! take! put!]]
            [secretary.core :as secretary :include-macros true]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]
            [consulate-simple.db :as db]
            [re-frame.db :refer [app-db]]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [markdown.core :refer [md->html]]
            [consulate-simple.consul :as consul]
            [consulate-simple.handlers]
            [consulate-simple.subs]
            [consulate-simple.partials :refer [navbar]]
            [consulate-simple.pages :refer [pages]]
            ;; [consulate-simple.db :as db]
            [ajax.core :refer [GET POST]])
  (:import goog.History))

(defn page []
  [(pages (or (session/get :page) :not-found))])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :page :home))

(secretary/defroute "/about" []
  (session/put! :page :about))

(secretary/defroute "/consul" []
  (dispatch-sync [:navigate :consul]))

(secretary/defroute "/consul/datacenters/:name" [name]
  (dispatch-sync [:navigate :detail name]))

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

;; use var refs for reloadability
(defn mount-components []
  (reagent/render-component [#'navbar] (.getElementById js/document "navbar"))
  (reagent/render-component [#'page] (.getElementById js/document "app")))




;; (defn init! []
;;   (consulate-simple.config/get-config)
;;   (hook-browser-navigation!)
;;   (secretary/dispatch! (.-hash js/window.location))
;;   (dispatch-sync [:initialise-db])
;;   (mount-components))

;; TODO do this next
(defn mount-root []
  (reagent/render [page]
                  (.getElementById js/document "app")))

(defn init! []
  (consulate-simple.config/get-config)
  ;(routes/app-routes)
  (secretary/dispatch! (.-hash js/window.location))
  (dispatch-sync [:initialize-db])
  (mount-components))
