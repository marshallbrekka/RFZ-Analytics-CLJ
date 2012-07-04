(ns analytics-clj.app.record-retriever.offset
  (:require [analytics-clj.app.mongo :as mongo]
            [analytics-clj.config :as config]))
(def offset-collection "timeseries-offsets")
(def db (apply mongo/connect (:mongo-offset config/conf)))
(def offsets 
  [{:key "no-offset" :label "No Offset"}
   {:key "date-joined" :label "Date Joined"}])


(defn get-json-spec []
  {:name      "offset" 
   :caption   "Event Offset" 
   :type      "select" 
   :options   (apply merge (map (fn [v] {(keyword (:key v)) (:label v)}) offsets))
  })

(defn run-query [args]
  (mongo/run-query db offset-collection args)) 


(defn get-offsets [type-key user-ids]
  (println type-key)
  (println (type type-key))
  (println (type (first user-ids)))
  (if (= type-key "no-offset") 
    (do
      (println "no offset running")
      {})
    (let [data (if (empty? user-ids) (run-query {:where {:type type-key}})
               (run-query {:where {:user-id {"$in" user-ids} :type type-key}}))]
      ;(println data)
          (apply merge (map (fn [a] {(keyword (str (:user-id a))) (:ts a)}) data)))))

(defn get-offset [data user-id]
  (if (or (empty? data) (not (contains? data user-id)))
    0
    (user-id data)))

