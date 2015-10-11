(ns consulate-simple.pages
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [clojure.string :refer [lower-case]]
            [consulate-simple.partials :as p]
            [consulate-simple.consul :as consul]
            [reagent.session :as session]
            [reagent-forms.core :refer [bind-fields]]
            [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [markdown.core :refer [md->html]]))

;; TODO consider move to app-db
(def nav-links {:home {:path "#/" :label "Home"}
                :about {:path "#/about" :label "About"}
                :consul {:path "#/consul" :label "Consul"}})


(defn nav-link [[name route]]
  [:li {:class (when (:active route) "active")}
   [:a {:href (:path route)} (:label route)]])

;; this can be treated like a page I guess
(defn navbar []
  (let [navigation (subscribe [:navigation])
        active_page (:page @navigation)
        nav-links-with-active (assoc-in nav-links [active_page :active] true)]
    [:div.navbar.navbar-inverse.navbar-fixed-top
     [:div.container
      [:div.navbar-header
       [:a.navbar-brand {:href "#/"} "myapp"]]
      [:div.navbar-collapse.collapse
       (into [:ul.nav.navbar-nav]
             (map nav-link nav-links-with-active))]]]))

(defn wrapper [& stuff]
  [:div.wrapper
   [p/header]
   [p/flash]
   (into [:div.container] stuff)])


(defn not-found []
  [:div "404 Not found"])

(defn about-page []
  [:div "this is the story of consulate-simple... work in progress"])

(defn home-page []
  [:div.container
   [:div.jumbotron
    [:h1 "Welcome to consulate-simple"]
    [:p "Time to start building your site!"]
    [:p [:a.btn.btn-primary.btn-lg {:href "http://luminusweb.net"} "Learn more »"]]]
   [:div.row
    [:div.col-md-12
     [:h2 "Welcome to ClojureScript"]]]
   (when-let [docs (session/get :docs)]
             [:div.row
              [:div.col-md-12
               [:div {:dangerouslySetInnerHTML
                      {:__html (md->html docs)}}]]])])

(defn datacenters-page []
  (let [datacenters (subscribe [:datacenters])]
    (wrapper
     (into [:div.datacenters]
           (map p/datacenter @datacenters)))))

;; (defn field-row [label input]
;;   [:div.row
;;     [:div.col-md-2 [:label label]]
;;     [:div.col-md-5 input]])

;; (defn add-parent [key value]
;;   (let [new-parent {:id key :title value :link "wut"}]
;;                                         ;  (swap! consulate-simple.core/app-state update-in [:detail :parents] conj new-parent)

;;   ))

;; (defn get-consul-kv [key & options]
;;   (go
;;       (prn options)
;;     (let [response (<! (consul/get-kv key options))]
;;       (prn (:status response))
;;       (prn (:body response)))))

;; (defn send-consul-kv [key value]
;;   (go
;;     (let [response (<! (consul/put-kv key value))]
;;       (prn (:status response))
;;       (prn (:body response))
;;       (if (= 200 (:status response))
;;         (add-parent key value)))))

;; (defn new-service-form-handler [doc event]
;;   (let [{:keys [consul-key consul-value] :as new-service} (:new-service-form @doc)
;;          result (send-consul-kv consul-key consul-value)]
;;     (if result
;;       (do
;;         (swap! doc assoc-in [:flash :success] "Key Successfully Saved")
;;         (swap! doc dissoc :new-service-form))
;;       (swap! doc update-in [:flash :error] "There was an error"))))

;; ;; (defn render-new-service-form []
;; ;;   (fn [doc]
;; ;;     (let [add-form-button [:button
;; ;;                             {:on-click #(swap! doc assoc :new-service-form {})}
;; ;;                             "Parents / Upstream (+)"]
;; ;;            the-form [:div.form {:id :new-service-form}
;; ;;                       [bind-fields
;; ;;                         [:div
;; ;;                           [:label "Key"]
;; ;;                           [:input {:field :text :id :new-service-form.consul-key}]
;; ;;                           [:label "Value"]
;; ;;                           [:input {:field :text :id :new-service-form.consul-value}]
;; ;;                           ]
;; ;;                         doc]
;; ;;                       [:button {:id :new-service-form.submit
;; ;;                                  :on-click #(new-service-form-handler doc %)} "submit"]]]

;; ;;       (if (contains? @doc :new-service-form)
;; ;;         the-form
;; ;;         add-form-button))))

(defn to-keyword [string]
  (keyword (clojure.string/lower-case (clojure.string/replace string " " "-" ))))

(defn simple-text-field [label & id]
  [:div.simple-text-field
   [:label label]
   [:input {:field :text :id (or (first id) (to-keyword label))}]])

(defn form-buttons [form event]
  [:button {:id :cancel
            :on-click #(dispatch [event false])} "Cancel"]
  [:button {:id :submit
            :on-click #(dispatch [event @form])} "Submit"])

(defn render-event-form [form]
  [:div.form {:id :event-form}
   [bind-fields
    [:div
     (simple-text-field "Name")
     (simple-text-field "Payload")
     (simple-text-field "Node Filter")
     (simple-text-field "Service Filter")
     (simple-text-field "Tag Filter")]
    form]
   (form-buttons form :submit-event-form)])

;; form should be a ratom
(defn render-new-service-form [form]
  [:div.form {:id :new-service-form}
;   [:div.errors (get-in @form [:new-service-form :errors])]
   [bind-fields
    [:div
     [:label "Key"]
     [:input {:field :text :id :new-service-form.consul-key}]
     [:label "Value"]
     [:input {:field :text :id :new-service-form.consul-value}]]
    form]
   [:button {:id :new-service-form.submit
             :on-click #(dispatch [:show-new-service-form false])} "Cancel"]
   [:button {:id :new-service-form.submit
             :on-click #(dispatch [:submit-new-service-form @form])} "Submit"]])

(defn render-add-form-button []
  [:button {:on-click #(dispatch [:show-new-service-form true])}
   "Parents / Upstream (+)"])

(defn new-service-form []
  (let [form (subscribe [:new-service-form])]
    (if (and form (:active @form))
      (render-new-service-form (reagent/atom @form)) ;;the reaction from re-frame won't work
      (render-add-form-button))))

(defn new-child-form []
  (let [form (subscribe [:new-service-form])]
    (if (and form (:active @form))
      (render-new-service-form (reagent/atom @form)) ;;the reaction from re-frame won't work
      (render-add-form-button))))

(defn event-button [label event]
  (prn label)
  (prn event)
  [:button {:on-click (fn [e] (do (prn e) (dispatch [event])))}
   label])

(defn event-form []
  (let [form (subscribe [:event-form])]
    (if (and form (:active @form))
      (render-event-form (reagent/atom @form)) ;;the reaction from re-frame won't work
      (event-button "Add Event" :show-event-form))))

(defn expand-child [event child-name doc]
  (println "expanding-child")
  (println event)
  (println child-name)
  (println doc)
  (go
    (let [{status :status nodes :body} (<! (consul/get-service-nodes child-name))]
      (swap! doc update-in [:detail :children :expanded] nodes))))


(defn middle-column [detail]
  [:div.middle-column
   [p/opc_box detail]])

(defn left-column [parents]
  [:div.left-column
   [:div.new-service-form
    [new-service-form]]
   [:div.parents
    (map p/row-parent parents)]])

(defn right-column [children]
  [:div.right-column
   [:div.new-child-form
    [event-form]]
   [:div.children
    [:div.child "hello world"]]])


(defn render-detail-page [detail]
  (wrapper
   [left-column (:parents @detail [])]
   [middle-column @detail]
   [right-column (:children @detail [])]

   ;; ; column 1
   ;; [:div.flexChild {:id "rowUpstream"}

   ;;  ]
   ;; ; column 2
   ;; [:div.flexChild {:id "rowDetailView"}
   ;;  [:div.dash_box
   ;;   [:div.opc_holder
   ;;    [:div.span.opc {:class "up"}
   ;;     p/image-spacer
   ;;     p/image-opcsprite]]
   ;;   [:div.h1 {:class "green"} (:name @detail)]
   ;;   (p/status-text "Healthy" "green")
   ;;   (p/opstate-text "Running" "green")
   ;;   (p/detail-buttons)]]
   ;; ; column 3
   ;; [into [:div.flexChild {:class "rowDownstream"}]
   ;;  (map (fn [child]
   ;;         (let [child_name (first child)]
   ;;           [:div.flexChild {:class "columnChild"}
   ;;            [:p.titles
   ;;             [:a {:href "javascript: void(0);"
   ;;                  :onclick #(dispatch [:expand-child %]) ;(fn [event] (expand-child event child_name doc) )
   ;;                  }
   ;;              (name child_name)]]
   ;;            ]))
   ;;       (:children @detail))]
   )
  )

(defn detail-page []
  (let [navigation (subscribe [:navigation])
        datacenters (subscribe [:datacenters])
        name (get-in @navigation [:args :name])
        ;;detail (first (filter #(= name (:name %)) @datacenters))
        detail (subscribe [:detail])]
    (if-not (nil? @detail)
      (do
        (render-detail-page detail))
      (not-found))))


;; use var refs for reloadability
(def pages
  {:home #'home-page
   :about #'about-page
   :not-found #'not-found
   :consul #'datacenters-page
   :detail  #'detail-page})

(defn current_page []
  "finds the function for a :page and puts it in a hiccup vector to be called reactively"
  (let [navigation (subscribe [:navigation])]
    [(pages (or (:page @navigation) :not-found))]))
