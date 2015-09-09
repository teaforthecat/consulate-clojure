(ns consulate-simple.pages
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [consulate-simple.partials :as p]
            [consulate-simple.consul :as consul]
            [reagent.session :as session]
            [reagent-forms.core :refer [bind-fields]]
            [re-frame.core :refer [subscribe]]
            [markdown.core :refer [md->html]]))

;; this can be treated like a page I guess
(defn navbar []
  (let [navigation (subscribe [:navigation])
        active_page (:page @navigation)]
    [:div.navbar.navbar-inverse.navbar-fixed-top
     [:div.container
      [:div.navbar-header
       [:a.navbar-brand {:href "#/"} "myapp"]]
      [:div.navbar-collapse.collapse
       [:ul.nav.navbar-nav
        [:li {:class (when (= :home active_page) "active")}
         [:a {:href "#/"} "Home"]]
        [:li {:class (when (= :about active_page) "active")}
         [:a {:href "#/about"} "About"]]
        [:li {:class (when (= :consul active_page) "active")}
         [:a {:href "#/consul"} "Consul"]]]]]]))


(defn about-page [doc]
  [:div "this is the story of consulate-simple... work in progress"])

(defn home-page [doc]
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
    [:div.wrapper
     [p/header]
     (into [:div.flexcontainer.wrap.column] ;;todo allow a "datacenters" div in css to contain them
           (map p/datacenter @datacenters))]))

(defn field-row [label input]
  [:div.row
    [:div.col-md-2 [:label label]]
    [:div.col-md-5 input]])

(defn add-parent [key value]
  (let [new-parent {:id key :title value :link "wut"}]
                                        ;  (swap! consulate-simple.core/app-state update-in [:detail :parents] conj new-parent)

  ))

(defn get-consul-kv [key & options]
  (go
      (prn options)
    (let [response (<! (consul/get-kv key options))]
      (prn (:status response))
      (prn (:body response)))))

(defn send-consul-kv [key value]
  (go
    (let [response (<! (consul/put-kv key value))]
      (prn (:status response))
      (prn (:body response))
      (if (= 200 (:status response))
        (add-parent key value)))))

(defn new-service-form-handler [doc event]
  (let [{:keys [consul-key consul-value] :as new-service} (:new-service-form @doc)
         result (send-consul-kv consul-key consul-value)]
    (if result
      (do
        (swap! doc assoc-in [:flash :success] "Key Successfully Saved")
        (swap! doc dissoc :new-service-form))
      (swap! doc update-in [:flash :error] "There was an error"))))

(defn new-service-form []
  (fn [doc]
    (let [add-form-button [:button
                            {:on-click #(swap! doc assoc :new-service-form {})}
                            "Parents / Upstream (+)"]
           the-form [:div.form {:id :new-service-form}
                      [bind-fields
                        [:div
                          [:label "Key"]
                          [:input {:field :text :id :new-service-form.consul-key}]
                          [:label "Value"]
                          [:input {:field :text :id :new-service-form.consul-value}]
                          ]
                        doc]
                      [:button {:id :new-service-form.submit
                                 :on-click #(new-service-form-handler doc %)} "submit"]]]

      (if (contains? @doc :new-service-form)
        the-form
        add-form-button))))

(defn expand-child [event child-name doc]
  (println "expanding-child")
  (println event)
  (println child-name)
  (println doc)
  (go
    (let [{status :status nodes :body} (<! (consul/get-service-nodes child-name))]
      (swap! doc update-in [:detail :children :expanded] nodes))))

;; (defn new-service-form []
;;   (fn [doc]
;;     [:div [:p [:a "hello"]]]))

(defn detail-page [doc]
  (let [detail (:detail @doc)]
    [:div.wrapper
     [p/header]
     [:div.content.flexChild.rowParent

      [:div.flexChild {:id "rowUpstream"}
       (map p/row-parent (:parents detail []) )
       [:div.flexChild {:id "columnChild75902"}
         [:p.titles
           [new-service-form doc]]]]


      [:div.flexChild {:id "rowDetailView"}
       [:div.dash_box
        [:div.opc_holder
         [:div.span.opc {:class "up"}
          p/image-spacer
          p/image-opcsprite]]
        [:div.h1 {:class "green"} (:name detail)]
        (p/status-text "Healthy" "green")
        (p/opstate-text "Running" "green")
        (p/detail-buttons)]]

      [into [:div.flexChild {:class "rowDownstream"}]
       (map (fn [child]
              (let [child_name (first child)]
                [:div.flexChild {:class "columnChild"}
                 [:p.titles
                  [:a {:href "javascript: void(0);"
                       :onclick (fn [event] (expand-child event child_name doc) )
                       }
                   (name child_name)]]
                 ]))
            (:children detail))]
      ;; [into [:div.flexChild {:class "rowDownstream"} ]
      ;;  (map
      ;;   (fn [{:keys [node address serviceport]}]
      ;;     [:div.flexchild.expanded node])

      ;;   (get-in detail [:children :expanded]))]

      ]]))

(defn not-found [doc]
  [:div "404 Not found"])

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
