(ns analytics-clj.app.record-retriever
   (:require
    [analytics-clj.app.mongo]
    [analytics-clj.app.record-retriever.disc :as disc]
    [analytics-clj.app.record-retriever.offset :as offset]
    [analytics-clj.app.record-retriever.sets :as sets]
    [analytics-clj.app.record-retriever.processing :as processing]
    [analytics-clj.app.record-retriever.internal :as internal]
    [analytics-clj.app.form-json :as form-json]

    ;[app.config :as config]
    ;[app.util.io :as io]
    [clojure.data.csv :as csv]
    [clojure.tools.logging :as lg]))



(defn now [] (java.util.Date.))
(defn log [msg]
  (println (now) msg))



(defn get-sets [] (sets/get-routes))
(defn get-offsets [] offset/offsets)
(defn get-form-spec [] 
  (form-json/build-json ["set" (sets/get-json-spec)] ["offset" (offset/get-json-spec)] ["render" (processing/get-json-spec)]))



(defn get-records-for-plot [route render offset]
  (log (str route " " render " " offset))
  (let [fns ((keyword (:render render)) processing/graph-types) 
        ids (sets/get-subset route)
        id-keywords (sets/ids-to-keywords ids)
        offsets (offset/get-offsets (keyword (:offset offset)) ids)
        user-points (internal/get-subset id-keywords (disc/deserialize-from-disc true))
       
        ;l (log id-keywords)
        l (log (count user-points))
        l (log (count (first user-points)))
        ;l (log (first user-points))
        data (if (empty? ids)
               {}
               (apply concat
                    (let [dat
                    (map (fn [[user-id points]]
                        (let [user-offset (offset/get-offset offsets user-id)]
                          (->> (map (fn [p] (internal/filter-point p user-offset)) points)
                               (filter internal/filter-nil)
                               ((:filter fns))))) user-points)]
                          
                      ;(log (str "sum " (reduce (fn [a b] (+ a (last (first b)))) 0 dat)))
                      ;(log dat)                      
                      dat)))]
                                       
    ;(log data)
    (log "filter completed")
    (log (count data))
    (log (type data))
    (log (first data))
    (internal/merge-data data (:merge fns) (:post-merge fns))))



(defn get-records-for-plot-seperate [route render offset]
  (let [fns ((keyword (:render render)) processing/graph-types) 
        ids (sets/get-subset route)
        id-keywords (sets/ids-to-keywords ids)
        offsets (offset/get-offsets (keyword (:offset offset)) ids)
        user-data (internal/get-subset id-keywords (disc/deserialize-from-disc false) (fn [a] (= (:type a) "credits")) false)
        l (log (count user-data))
        data (map (fn [[user-id points]]
                (let [user-offset (offset/get-offset offsets user-id)
                      redat
                  (map (fn [v]
                      (->> (map #(internal/filter-point % user-offset) v)
                           (filter internal/filter-nil))) 
                      points)] 
                  redat)) user-data)]

    (log "filter completed")
    (log (count data))
    (log (count (first data)))
    (log (first data))
    data))




(defn get-records [plots]
  (filter (fn [v] (not= v nil)) (reduce (fn [a b] (apply conj a b)) [] (map (fn [[k v]]
         (if (= (:render v) "accounts")
           (reduce (fn [a b]
                    (if (empty? b)
                     a
                     (apply conj a b))) [] (get-records-for-plot-seperate (:set v) (:render v) (:offset v)))
           [(get-records-for-plot (:set v) (:render v) (:offset v))])) plots))))

