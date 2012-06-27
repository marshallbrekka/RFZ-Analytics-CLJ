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
     [:h3 "Quick Start"]
     [:p "Press submit"]
     [:h2 "User Sets"]
     [:p "Find users by date they joined: The start and end values take unix timestamps in milliseconds. You can use " [:a {:href "http://www.epochconverter.com/"} "Epoch Converter"] " for now to generate the timestamps, just remember to add three zeros to the end of the timestamp it gives you back. In the future this will just be a date picker."]
     [:h2 "Data Aggregation"] 
     [:p "Total Balances: This method displays the sum of all users balances per day."]
     [:p "Mean Balance Rescaling: This method sums the percentage of change for each users balance per day."]
     [:p "Percent Change from Start: This method calculates percentage of change for a users balance based on their starting balance, and sums the percentages for all users."]
     [:p "Accounts (Debugging): This method plots all of the accounts found within the set of users. It is mostly for bebugging purposes. If you use it try to keep your user sets small, otherwise it just becomes a mess of lines."]
     [:h2 "Adding plots"]
     [:p "You can add or remove sets to be plotted using the \"Add Plot\" and \"Remove\" links."]]
     [:div#data-form]
     [:img#loader {:src "img/ajax.gif"}]
     [:div#container]
     [:script (str "var offsets = " 
                   (json/generate-string 
                     (map (fn [item] {:value (:key item) :label (:label item)}) (rr/get-offsets)))
                   "; var sets = " 
                   (json/generate-string (rr/get-sets))
                   "; var renderModes = [
                   {value : 'total', label : 'Total Balances'}, 
                   {value : 'average', label : 'Mean-Balance Rescaling'},
                   {value : 'average-from-start', label : 'Percent Change from Start'},  
                   {value : 'accounts', label : 'Accounts (Debugging)'}]; run(); //")]]))

(defpage [:get "/api"] {:keys [plots]}
  (resp/json (record-retriever/get-records plots)))

(defpage [:get "/run"] []
  (resp/json (rrd/serialize-from-mongo true)))
     
