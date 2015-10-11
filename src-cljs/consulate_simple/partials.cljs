(ns consulate-simple.partials
  (:require [consulate-simple.config :refer (config)]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :refer [atom]]
            [reagent.session :as session]))

(def color-states {"Failing" "red"
                   "Healthy" "green"})

(def color-op-states {"down"    "yellow"
                      "active"  "green"
                      "standby" "yellow"
                      "test"    "yellow"})

(def operational-states {"down"    "down"
                         "active"  "up"
                         "standby" "standby"
                         "test"    "test"
                         "down2"   "down2"})

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
  [:h3  {:class color}
   [:span "Status:"]
   [:span text ]])

(defn opstate-text [text color]
  [:h3  {:class color}
   [:span "OP State:"]
   [:span text ]])

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
    [:div.datacenter
     [:a.detail_title {:class main_color
                       :href (str "#/consul/datacenters/" name)}
      name]
     [:div.detail_state_text [status-text stx main_color]]
     [:div.detail_state_text [opstate-text otx op_color]]
     [:div.opc {:class "down2"}  ]]))

(defn opc_box [detail]
  (let [name "dc2"
        health_state "down"
        op_state "up"
        stx "wut"
        otx "who"
        main_color "green"
        op_color "yellow"]
    [:div.opc_box
     [:a.detail_title {:class main_color
                       :href (str "#/consul/datacenters/" name)}
      name]
     [:div.detail_state_text [status-text stx main_color]]
     [:div.detail_state_text [opstate-text otx op_color]]
     [:div.opc {:class op_state}  ]]))


(defn header []
  [:div.header
    [:div.consulate_logo
     [:a {:href (:root-path config)}
      (image-header-logo)]]])

(defn flash []
  (let [{:keys [state message]} @(subscribe [:flash])]
    [:div.flash {:class state} message ]))

(defn row-parent [{:keys [title link value]}]
  (let [id (str "123" (clojure.string/join "" (take 4 (repeatedly #(rand-nth "123456")))))
        ;;    toggler (atom "hide")
        ;; the key has / in it
        consul-key (clojure.string/join "/" (filter string? [(namespace title) (name title)]))
        short-name (name title)
        ]
    [:div.parent {:key id}
     [:div.spacer " "]
     [:div.consul-key ;{:on-click #(reset! toggler (fn [t] (if (= t "hide") "show" "hide")))}
      consul-key]
     [:div.delete-object
      [:button {:on-click #(dispatch [:delete-kv consul-key])} "X"]]
     [:div.parent-value {:class "hide"}
      (str value)]]))

(defn related-element [{:keys [title link]}]
  [:div.flexChild {:class "related-element"}
   [:p.titles
    [:a {:href link} title]]])
