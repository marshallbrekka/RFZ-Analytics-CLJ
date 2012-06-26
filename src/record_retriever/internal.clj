(ns record-retriever.internal)
(defn now [] (java.util.Date.))
(defn log [msg]
  (println (now) msg))

(defn filter-nil [point]
  (not (nil? point)))




(defn get-day [ts]
  (let [ts 
        (if (> ts 1000000000000)
          (/ ts 1000)
          ts)]
    (* (int (Math/floor (float (/ ts (* 60 60 24))))) 60 60 24 1000)))


(defn filter-point [point offset]
 ;(log (str point offset)) 
    (when (and (first point) (not= (first point) 0))
      (let [
            balance (last point) 
            ts (- (first point) (get-day offset))]
      [ts balance])))
 




(defn get-subset 
  ([user-ids data filter-fn flat?]
  ;(log (str (type user-ids) user-ids))
  ;(log (str "uids " (count user-ids) " " (count data)))
  (if (empty? user-ids)
    data
    (reduce (fn [new-map user-id] 
              (if (contains? data user-id)
                  (merge new-map {user-id (let [filtered (filter filter-fn (user-id data))]
                                            ;(log filtered)
                                  (if flat? (:points (first filtered)) (map :points filtered)))}) 
                  new-map)) {} user-ids)))
  ([user-ids data] (get-subset user-ids data (fn [a] true) true)))

(defn merge-data [data merge-fn post-merge-fn]
  (log (str "merge start " (count data)))
  (let [data (sort-by first data)
        l (log "sorted")
        data (filter (fn [pt] 
                           (if (nil? pt)
                             (println "found nil in filter"))
                             (not (nil? pt))) data)


    
    l (log "sort complete, merging")
        merged (pmap merge-fn (partition-by (fn [a] 
          (if (nil? a)
            (println "partition by found nil item")
            (if (nil? (first a))
              (println "partition by first found nil")))
                                  (first a)) data))
    l (log "merged")
    l (log (take 10 merged))

    ;l (log merged)
    
    l (log "merged complete, starting post-merge")
    ;l (log (take 10 merged))
    ;l (log (str "number of points " (count merged)))

    re (reduce post-merge-fn nil merged)]
    (log "end")
    re)) 



