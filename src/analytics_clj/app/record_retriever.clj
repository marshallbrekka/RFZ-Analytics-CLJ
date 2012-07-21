(ns analytics-clj.app.record-retriever
   (:require
    [analytics-clj.app.mongo]
    [analytics-clj.app.record-retriever.disc :as disc]
    [analytics-clj.app.record-retriever.offset :as offset]
    [analytics-clj.app.record-retriever.sets :as sets]
    [analytics-clj.app.record-retriever.processing :as processing]
    [analytics-clj.app.record-retriever.internal :as internal]
    [analytics-clj.app.form-json :as form-json]
    [analytics-clj.app.record-retriever.batching :as batching]

    ;[app.config :as config]
    ;[app.util.io :as io]
    [clojure.data.csv :as csv]
    [clojure.tools.logging :as lg]))


(defn now [] (java.util.Date.))
(defn log [& msg]
  (apply println (now) msg))

(defn log-out [& msg]
  (apply println (now) (butlast msg))
  (last msg))

(defn logp 
  ([v obj]
   (println v obj)
   obj)
  ([v] (logp v v)))



(defn get-form-spec [] 
  (form-json/build-json 
    ["set" (sets/get-json-spec)] 
    ["offset" (offset/get-json-spec)] 
    ["render" (processing/get-json-spec)]
    ["batch" (batching/get-json-spec)]))

(defn get-plot-description [route render offset num-users]
  (log "route: " route)
  (log "offset: " offset)
  (log "render: " render)
  {:count   num-users 
   :offset  (offset/get-description (:offset offset))
   :set     (sets/get-description route)
   :type    (processing/get-description (keyword (:render render)))})

(defn filter-timelines [filter-fn offset timelines id]
  (map (fn [timeline] 
         (-> (update-in timeline [:points] 
                        #(->> (map (fn [point] (internal/apply-offset offset point)) %)
                              (filter internal/filter-nil)
                              (filter-fn)))
             (merge {:uid (read-string (name id))}))) timelines))

(defn merge-batches [merge-fn post-merge-fn final-fn batches]
  (map (fn [batch]
          (update-in batch [:timelines]
            #(internal/merge-data
                %
                merge-fn
                post-merge-fn
                final-fn))) batches))

(defn build-plot-spec [route render offset num-ids batches]
   {:info    (get-plot-description route render offset num-ids)
    :batches  batches})

(defn filter-out-empty-timelines [users]
  (->> (map (fn [timelines] 
              (filter #(-> (:points %)
                           ((fn [a] (or (empty? a) (nil? (first a)))))
                           (not= true))
                      timelines))
            users)
       (filter #(-> (empty? %)
                    (not= true)))))

(defn process-plot-data [user-points offsets batch-type fns]
   ;(println user-points)
   (->> (map (fn [[id timelines]] 
              (filter-timelines
                (:filter fns) 
                (offset/get-offset offsets id)
                timelines id))
            user-points)
        (log-out "applied offsets")
        (filter-out-empty-timelines)
        (log-out "filtered empty")
        ;(#(log-out % %))
        (batching/batch batch-type)
        (log-out "batched")
        (merge-batches (:merge fns) (:post-merge fns) (:final fns))))


(defn get-plot
  ([route render offset batch]
    (let [type-key (internal/get-type-key batch)
          fns ((keyword (:render render)) processing/graph-types)
          l (log "pre ids")
          ids (sets/get-subset route)
          ;l (log ids)
          l (log "post ids")
          id-keywords (sets/ids-to-keywords ids)
          l (log "post-keywords")
          offsets (offset/get-offsets (keyword (:offset offset)) ids)
          l (log "post offsets")
          user-points (internal/get-subset id-keywords (disc/deserialize-from-disc type-key))
          l (log "post user-points")]

      (->> (process-plot-data user-points offsets batch fns)
           (build-plot-spec route render offset (count ids)))))
  ([route render offset] (get-plot route render offset :merged)))


(def get-plot-memo (memoize get-plot))  


(defn get-records [plots]
  ;(set! *warn-on-reflection* true)
  (map (fn [[k v]]
          (get-plot-memo (:set v) (:render v) (:offset v) (if (contains? v :batch)
                                                              (keyword (:batch (:batch v)))
                                                              :merged))) plots))
