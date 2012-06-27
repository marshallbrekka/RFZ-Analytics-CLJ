(ns record-retriever
   (:require
    [mongo]
    [record-retriever.disc :as disc]
    [record-retriever.offset :as offset]
    [record-retriever.sets :as sets]
    [record-retriever.processing :as processing]
    [record-retriever.internal :as internal]

    ;[app.config :as config]
    ;[app.util.io :as io]
    [clojure.data.csv :as csv]
    [clojure.tools.logging :as lg]))



(defn now [] (java.util.Date.))
(defn log [msg]
  (println (now) msg))



(defn get-sets [] (sets/get-routes))
(defn get-offsets [] offset/offsets)



(defn get-records-for-plot [route setoptions render offset]
  (let [fns (render processing/graph-types) 
        ids (sets/get-subset route setoptions)
        id-keywords (sets/ids-to-keywords ids)
        offsets (offset/get-offsets offset ids)
        user-points (internal/get-subset id-keywords (disc/deserialize-from-disc true))
       
        ;l (log id-keywords)
        l (log (count user-points))
        l (log (count (first user-points)))
        ;l (log (first user-points))
        data (apply concat
                    (let [dat
                    (map (fn [[user-id points]]
                        (let [user-offset (offset/get-offset offsets user-id)]
                          (->> (map (fn [p] (internal/filter-point p user-offset)) points)
                               (filter internal/filter-nil)
                               ((:filter fns))))) user-points)]
                          
                      (log (str "sum " (reduce (fn [a b] (+ a (last (first b)))) 0 dat)))
                      ;(log dat)                      
                      dat))]
                                       
    ;(log data)
    (log "filter completed")
    (log (count data))
    (log (type data))
    (log (first data))
    (internal/merge-data data (:merge fns) (:post-merge fns))))



(defn get-records-for-plot-seperate [route setoptions render offset]
  (let [fns (render processing/graph-types) 
        ids (sets/get-subset route setoptions)
        id-keywords (sets/ids-to-keywords ids)
        ;l (log (str "id keywords " (count ids)))
        offsets (offset/get-offsets offset ids)
        user-data (internal/get-subset id-keywords (disc/deserialize-from-disc false) (fn [a] (= (:type a) "credits")) false)
        l (log (count user-data))
        data (map (fn [[user-id points]]
                (let [user-offset (offset/get-offset offsets user-id)
                      redat
                  (map (fn [v]
                         ;(log (str "v " (first v)))
                         (->> (map #(internal/filter-point % user-offset) v)
                              (filter internal/filter-nil))) 
                       points)] 
                  ;(log (str "post v " (first redat)))
                  redat)) user-data)]

    (log "filter completed")
    (log (count data))
    (log (count (first data)))
    (log (first data))
    data))




(defn get-records [plots]
  (reduce (fn [a b] (apply conj a b)) [] (map (fn [[k v]]
         (if (= (:render v) "accounts")
           (reduce (fn [a b]
                    (if (empty? b)
                     a
                                         (apply conj a b))) [] (get-records-for-plot-seperate (:set v) (:set-options v) (keyword (:render v)) (keyword (:offset v))))
           [(get-records-for-plot (:set v) (:set-options v) (keyword (:render v)) (keyword (:offset v)))])) plots)))





