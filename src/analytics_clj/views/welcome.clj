(ns analytics-clj.views.welcome
  (:require [analytics-clj.views.common :as common]
            [noir.response :as resp]
            [record-retriever :as rr]
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
      [:form#data-form
      ;[:label {:for "user_ids"} "User Ids to render (blank renders all)"]
      ;[:input#user_ids {:type "text" :name "user_ids"}]
      [:label {:for "user-set"} "User Set "]
      [:br]
      [:select#user-set {:name "set"}
       (map (fn [[k v]] 
            [:option {:value k} (:name v)]) (rr/get-sets))]
      [:select#offset {:name "offset"}
       (map (fn [offset] [:option {:value (:key offset)} (:label offset)]) (rr/get-offsets))]
      [:br]
      [:input {:type "radio" :name "render_type" :value "total"} "Total Balances"]
      [:br]
      [:input {:type "radio" :name "render_type" :value "average"} "Percentage Change"]
      [:br]
      [:input {:type "submit"}]]
     [:img#loader {:src "img/ajax.gif"}]
     [:div#container]
     [:script "run(); //"]]))

(defpage [:get "/api"] {:keys [offset id-set render_type]}
  (resp/json (record-retriever/get-records id-set (keyword render_type) offset)))

(defpage [:get "/run"] []
  (resp/json (rrd/serialize-from-mongo)))
     
