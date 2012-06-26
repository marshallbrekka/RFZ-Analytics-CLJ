(ns analytics-clj.views.welcome
  (:require [analytics-clj.views.common :as common]
            [noir.response :as resp]
            [record-retriever :as rr]
            [cheshire.core :as json]
            [record-retriever.disc :as rrd])
  (:use [noir.core] [hiccup.core] ))

(defpage "/welcome" []
         (common/layout
           [:p "Welcome to analytics-clj"]))

 (defpage "/hello" [] "hello")




(defpage "/" []
  (html
    [:head
     [:title "ReadyForZero Datalytics (alpha)"]
     [:script {:src "js/plugins/jquery-1.7.2.min.js"}]
     [:script {:src "js/plugins/highstock.js"}]
     [:script {:src "js/api.js"}]
     [:script {:src "js/Graph.js"}]
     [:link {:href "css/styles.css" :rel "stylesheet" :type "text/css"}]]
    [:body 
     [:div#desc
     [:h1 "ReadyForZero Datalytics (alpha)"]
     [:p "Use the form options to view time series for subsets of users. For now only the balance is displayed."]
     [:h2 "Data Aggregation"]  
     [:p "Total Balances: This method displays the sum of all users balances per day."]
     [:p "Percentage Change: This method sums the percentage of change for each users balance per day."]]
     [:div#data-form]
     [:img#loader {:src "img/ajax.gif"}]
     [:div#container]
     [:script (str "var offsets = " 
                   (json/generate-string 
                     (map (fn [item] {:value (:key item) :label (:label item)}) (rr/get-offsets)))
                   "; var sets = " 
                   (json/generate-string (rr/get-sets))
                   "; var renderModes = [{value : 'total', label : 'Total Balances'}, {value : 'average', label : 'Percentage Change'}, {value : 'accounts', label : 'accounts'}]; run(); //")]]))

(defpage [:get "/api"] {:keys [plots]}
  (resp/json (record-retriever/get-records plots)))

(defpage [:get "/run"] []
  (resp/json (rrd/serialize-from-mongo true)))
     
