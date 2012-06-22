(ns record-retriever.sets
  (:require [mongo]))
(def sets-collection "usersets")
(def conn (mongo/connect "analytics"))
(def sets (merge (reduce (fn [n s]
        (merge n {(keyword (:id s)) {:id (:id s) :name (:name s) :ids (:ids s)}}))
          {} (mongo/run-query conn sets-collection {}))  {:4 {:id 4 :name "test" :ids [1,2598]}}  {:5 {:id 5 :name "nacho" :ids [2598]}}  {:6 {:id 6 :name "R" :ids [1]}}))
(defn ids-to-keywords [ids]
  (map (fn [a] (keyword (str (int a)))) ids))

