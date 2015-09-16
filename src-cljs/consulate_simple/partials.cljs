(ns consulate-simple.partials
  (:require [consulate-simple.config :refer (config)]
            [re-frame.core :refer [subscribe]]
            [reagent.core :refer [atom]]
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
      [status-text stx main_color]
      [opstate-text otx op_color]
      [detail-buttons]]]))

(defn header []
  [:div.header
    [:div.consulate_logo
     [:a {:href (:root-path config)}
      (image-header-logo)]]])

(defn flash []
  (let [{:keys [state message]} @(subscribe [:flash])]
    [:div.flash {:class state} message ]))

(defn row-parent [{:keys [id title link key value]}]
  (let [id (str "123" (clojure.string/join "" (take 4 (repeatedly #(rand-nth "123")))))
        ;toggler (atom "hide")
        ]
    [:div.flexChild {:id id :key id}
     [:p.titles
      [:span ;{:on-click #(reset! toggler (fn [t] (if (= t "hide") "show" "hide")))}
       title]
      [:div.parent-value {:class "hide"}
       value]]]))

(defn related-element [{:keys [title link]}]
  [:div.flexChild {:class "related-element"}
   [:p.titles
    [:a {:href link} title]]])
