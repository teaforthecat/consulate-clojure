(ns consulate-simple.pages
  (:require [consulate-simple.partials :as p]
            [reagent.session :as session]
            [reagent-forms.core :refer [bind-fields]]
            [markdown.core :refer [md->html]]))


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

(defn datacenters-page [doc]
  [:div.wrapper
   [p/header]
   (into [:div.flexcontainer.wrap.column] ;;todo allow a "datacenters" div in css to contain them
         (map p/datacenter (:datacenters @doc)))])


(defn detail-page [doc]
  (let [detail (:detail @doc)]
    [:div.wrapper
     [p/header]
     [:div.content.flexChild.rowParent

      [:div.flexChild {:id "rowUpstream"}
       (map p/row-parent (:parents detail []) )
       [:div.flexChild {:id "columnChild75902"}
        [:p.titles
         [:button
          {:on-click #(swap! doc assoc :new-service-form true)}
          "Parents / Upstream (+)"]
         (if (true? (:new-service-form @doc))
           [:div.form
            [bind-fields [:input {:field :text :id :new-service}] doc]
            [:button {:on-click #(swap! doc dissoc :new-service-form)}
             "Enter"]])
         ]]
       (p/row-parent {:id "new-service" :title (:new-service @doc) :link "" })

       ]


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

      [:div.flexChild {:id "rowDownstream"}

       [:div.flexChild {:id "columnChild86333"}
        [:p.titles
         [:a {:href "/"} "Children / Downstream"]]]

       [:div.flexChild {:id "columnChild86333"}
        [:p.titles
         [:a {:href "/"} "A process Name"]]]]]]))


(def pages
  {:home #'home-page
   :about #'about-page
   :datacenters #'datacenters-page
   :detail  #'detail-page})
