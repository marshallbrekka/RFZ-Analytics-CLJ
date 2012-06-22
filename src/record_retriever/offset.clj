(ns record-retriever.offset
  (:require [mongo]))
(def offset-collection "timeseries-offsets")
(def db (mongo/connect "mydb" "10.10.10.106" 27019))
(def offsets 
  [{:key "no-offset" :label "No Offset"}
   {:key "date-joined" :label "Date Joined"}])



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

