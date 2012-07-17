(ns analytics-clj.app.record-retriever.internal
  (:require [analytics-clj.app.record-retriever.batching :as batching]))
(defn now [] (java.util.Date.))
(defn log [& msg]
  (do (apply println (now) msg)))
(defn log-out [& args]
  (do (apply log (butlast args))
      (last args)))

(defn logp [obj]
  (println obj)
  obj)

(defn filter-nil [point]
  (not= point nil))

(defn get-day [ts]
  (let [day (* 60 60 24)]
    (-> (if (> ts 1000000000000)
            (/ ts 1000)
            ts)
        (/ day)
        (double)
        (Math/floor)
        (int)
        (* day 1000))))

(defn build-plot [points options info]
  {:points points 
   :options options 
   :info info})


(defn get-type-key [batch]
  (if (:accounts (batch batching/types))
      :seperate
      :merged))


(defn apply-offset [offset point ]
    (when (and (first point) 
               (not= (first point) 0) 
               (filter-nil offset))
      (let [balance (last point) 
            ts (- (first point) (get-day offset))]
        [ts balance])))



(defn get-subset 
  ([user-ids data filter-fn]
  (log "uids " (count user-ids) " " (count data))
  (if (empty? user-ids)
      '()
      (reduce (fn [new-list user-id] 
              (if (contains? data user-id)
                  (let [timelines (filter filter-fn
                                    (user-id data))]
                    (if (empty? timelines)
                        new-list
                        (merge new-list
                               {user-id timelines})))
                  new-list)) 
            {}
            user-ids)))
  ([user-ids data] (get-subset user-ids data (fn [a] true))))

(defn find-first-day [timelines]
  (reduce (fn [prev timeline]
            (if ( < (first (first timeline)) prev)
                (first (first timeline))
                prev))
          (first (first (first timelines))) (rest timelines)))

(defn get-vector-index-from-day [offset day]
  (/ (- day offset) 86400000))

(defn put-to-days [timelines]
  (let [start-day (find-first-day timelines)
        days (vec (repeatedly 1042 #(atom '())))]
    (dorun (pmap (fn [timeline]
                    (dorun (pmap (fn [day] 
                                 (if (nil? (first day))
                                   (log (str "nil timeline day : " timeline))
                                    (swap! (get days (get-vector-index-from-day start-day (first day))) conj day)))
                              timeline)))
                timelines))
    (log "pre into")
    days))


(defn merge-data [data merge-fn post-merge-fn]

  (log (str "merge start " (count data)))
  ;(log (str "first day " (find-first-day data)))
  (->> (put-to-days data)
       ;(reduce #(apply conj % %2) '() data)
       (log-out "reduced")
       ;(sort-by first)
       ;(log-out "sorted")
       ;(filter filter-nil)
       ;(log-out "filtered nil, merging")
       ;(partition-by first)
       ;(logp)
       (filter (fn [a] (not= true (empty? @a))))
       (log-out "filtered empty vector places")
       (map (fn [v] (merge-fn @v)))
       (log-out "merged, starting post merge")
       (reduce post-merge-fn nil)
       (log-out "post merge complete")))


