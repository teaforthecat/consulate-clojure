(ns consulate-simple.pages
  (:require [consulate-simple.partials :as p]
            [reagent.session :as session]
            [markdown.core :refer [md->html]]))


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

(defn datacenters-page [doc]
  [:div.wrapper
   [p/header]
   (into [:div.flexcontainer.wrap.column] ;;todo allow a "datacenters" div in css to contain them
         (map p/datacenter (:datacenters doc)))])


(defn detail-page [doc]
  [:div.wrapper
   [p/header]
   [:div.content.flexChild.rowParent

    [:div.flexChild {:id "rowUpstream"}

     [:div.flexChild {:id "columnChild86333"}
      [:p.titles
       [:a {:href "/"} "Parents / Upstream"]]]

     [:div.flexChild {:id "columnChild75902"}
      [:p.titles
       [:a {:href "/"} "Another Process"]]]]


    [:div.flexChild {:id "rowDetailView"}
     [:div.dash_box
      [:div.opc_holder
       [:div.span.opc {:class "Running"}
        p/image-spacer
        p/image-opcsprite]]
      [:div.h1 {:class "green"} "Eden Prairie"]
      (p/status-text "Healthy" "green")
      (p/opstate-text "Running" "green")
      (p/detail-buttons)]]

    [:div.flexChild {:id "rowDownstream"}

     [:div.flexChild {:id "columnChild86333"}
      [:p.titles
       [:a {:href "/"} "Children / Downstream"]]]

     [:div.flexChild {:id "columnChild86333"}
      [:p.titles
       [:a {:href "/"} "A process Name"]]]]]])


(def pages
  {:home #'home-page
   :about #'about-page
   :datacenters #'datacenters-page
   :detail  #'detail-page})
