(ns analytics-clj.app.record-retriever.offset
  (:require [analytics-clj.app.mongo  :as mongo]
            [analytics-clj.config     :as config]))
(defn now [] (java.util.Date.))
(defn log [msg]
  (println (now) msg))


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
  (log (type user-ids))
  (log (type (first user-ids)))
  (if (= type-key :no-offset) 
      {}
      (let [data (if (empty? user-ids) 
                     (run-query {:where {:type type-key}})
                     (do
                       (log "pre query")
                     (doall (run-query {:where {:user-id {"$in" user-ids} :type type-key}}))))]
        (do
          (log "post query")
        (apply merge (map (fn [a] {(keyword (str (:user-id a))) (:ts a)}) 
                          data))))))

(defn get-offset [data user-id]
  (cond (empty? data) 0
        (contains? data user-id) (user-id data)
        :else nil))


(defn get-description [type-key]    
  (str "Offset: " 
       (:label 
            (first 
              (filter 
                (fn [item] (= (:key item) type-key)) 
                offsets)))))

