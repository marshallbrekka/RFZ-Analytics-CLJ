(ns analytics-clj.views.welcome
  (:require [analytics-clj.views.common :as common]
            [noir.response :as resp]
            [record-retriever :as rr])
  (:use [noir.core] [hiccup.core] ))

(defpage "/welcome" []
         (common/layout
           [:p "Welcome to analytics-clj"]))

 (defpage "/hello" [] "hello")

(defpage "/" []
  (html
    [:head
     [:script {:src "js/plugins/jquery-1.7.2.min.js"}]
     [:script {:src "js/plugins/highstock.js"}]
     [:script {:src "js/api.js"}]
     [:script {:src "js/Graph.js"}]
     [:link {:href "css/styles.css" :rel "stylesheet" :type "text/css"}]]
    [:body 
     [:img#loader {:src "img/ajax.gif"}]
     [:div#container]
     [:script "getData()"]]))

(defpage [:get "/api"] []
  (resp/json (record-retriever/get-using-user-ids-batch :total)))
     
