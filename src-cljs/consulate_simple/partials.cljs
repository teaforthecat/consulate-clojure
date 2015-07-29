(ns consulate-simple.partials
  (:require [consulate-simple.config :refer (config)]
            [reagent.session :as session]))

(def color-states {"Failing" "red"
                   "Healthy" "green"})

(def color-op-states {"down"    "yellow"
                      "active"  "green"
                      "standby" "yellow"
                      "test"    "yellow"})

(def operational-states {"down" "down"
                         "active" "up"
                         "standby" "up"
                         "test" "test"})


(defn image-header-logo []
  [:img {:alt "Consulate Dashboard Logo"
         :border "0"
         :src "img/consulate_logo.png"
         :title "&amp;lt; Back to Dashboard"
         :width "300em"}])

(defn navbar []
  [:div.navbar.navbar-inverse.navbar-fixed-top
   [:div.container
    [:div.navbar-header
     [:a.navbar-brand {:href "#/"} "myapp"]]
    [:div.navbar-collapse.collapse
     [:ul.nav.navbar-nav
      [:li {:class (when (= :home (session/get :page)) "active")}
       [:a {:href "#/"} "Home"]]
      [:li {:class (when (= :about (session/get :page)) "active")}
       [:a {:href "#/about"} "About"]]
      [:li {:class (when (= :datacenters (session/get :page)) "active")}
       [:a {:href "#/consul"} "Consul"]]]]]])


(def image-spacer
  [:img.spacer {:src "img/spacer.png"}])

(def image-opcsprite
  [:img.opcsprite {:src "img/opstate.png"}])


(defn status-text [text color]
  [:div.statustext
   [:p.status "Status:  "]
   [:p.status {:class color} text ]])

(defn opstate-text [text color]
  [:div.optext
   [:p.opstate "OP State:  "]
   [:p.status {:class color} text ]])

(defn detail-buttons []
  [:div.detail_buttons
   [:input.detail_button {:type "Submit"}]])

(defn datacenter [dc]
  (let [name (:name dc)
        stx (:health_status dc)
        otx (:op_state dc)
        main_color (or (get color-states stx) "unknown")
        op_color (or (get color-op-states otx ) "unknown")
        op_state (or (get operational-states otx) "unknown")]
    [:div.div.datacenter ; todo swap div for datacenter in css
     [:div.dash_box
      [:div.opc_holder
       [:div.span.opc {:class op_state}
        image-spacer
        image-opcsprite]]
      [:a {:href  (str "#/consul/datacenters/" name)
           ;(routes/path-for :detail-page)
           }
       [:div.h1 {:class main_color} name]]
       (status-text stx main_color)
      (opstate-text otx op_color)
      (detail-buttons)]]))


(defn header []
  [:div.header
    [:div.consulate_logo
     [:a {:href (:root-path config)}
      (image-header-logo)]]])


(defn row-parent [{:keys [id title link]}]
  [:div.flexChild {:id id :key id}
   [:p.titles
    [:a {:href link} title]]])
