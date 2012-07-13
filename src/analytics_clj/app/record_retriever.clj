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


(defn get-form-spec [] 
  (form-json/build-json 
    ["set" (sets/get-json-spec)] 
    ["offset" (offset/get-json-spec)] 
    ["render" (processing/get-json-spec)]))

(defn get-plot-description [route render offset num-users]
  (log "route: " route)
  (log "offset: " offset)
  (log "render: " render)
  {:count   num-users 
   :offset  (offset/get-description (:offset offset))
   :set     (sets/get-description route)
   :type    (processing/get-description (keyword (:render render)))})

(defn filter-timelines [filter-fn offset timelines]
  (map (fn [timeline] 
         (update-in timeline [:points] 
                    #(->> (map (fn [point] (internal/apply-offset offset point)) %) 
                          (filter internal/filter-nil)
                          (filter-fn)))) timelines))

(defn merge-batches [merge-fn post-merge-fn batches]
  (map (fn [batch]
          (update-in batch [:timelines] 
            #(internal/merge-data 
                %
                merge-fn 
                post-merge-fn))) batches))

(defn build-plot-spec [route render offset num-ids batches]
   {:info    (get-plot-description route render offset num-ids)
    :batches  batches})

(defn filter-out-empty-timelines [users]
  (->> (map (fn [timelines] 
              (filter #(-> (:points %)
                           (empty?)
                           (not= true))
                      timelines))
            users)
       (filter #(-> (empty? %)
                    (not= true)))))

(defn process-plot-data [user-points offsets batch-type fns]
  (log "user points")
  (log user-points)
  (->> (map (fn [[id timelines]] 
              (filter-timelines
                (:filter fns) 
                (offset/get-offset offsets id)
                timelines))
            user-points)
        (filter-out-empty-timelines)
        (batching/batch batch-type)
        (merge-batches (:merge fns) (:post-merge fns))))


(defn get-plot
  ([route render offset batch]
    (let [type-key (internal/get-type-key batch)
          fns ((keyword (:render render)) processing/graph-types)
          ids (sets/get-subset route)
          id-keywords (sets/ids-to-keywords ids)
          offsets (offset/get-offsets (keyword (:offset offset)) ids)
          user-points (internal/get-subset id-keywords (disc/deserialize-from-disc type-key))]

      (->> (process-plot-data user-points offsets batch fns)
           (build-plot-spec route render offset (count ids)))))
  ([route render offset] (get-plot route render offset :merged)))


    


(defn get-records [plots]
  (map (fn [[k v]]
          (get-plot (:set v) (:render v) (:offset v))) plots))

