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
        ;l (log ids)
        ids (sets/get-subset route setoptions)
        ;l (log ids)
        id-keywords (sets/ids-to-keywords ids)
        offsets (offset/get-offsets offset ids)
        ;l (log offsets)
        ;l (log ids)
        data (apply concat (map (fn [[user-id points]]
                           (let [user-offset (offset/get-offset offsets user-id)]
                           ((:filter fns) (filter internal/filter-nil (map (fn [p] (internal/filter-point p user-offset)) points)))))
                                (internal/get-subset id-keywords 
                                      (disc/deserialize-from-disc))))]
    (log "filter completed")
    (internal/merge-data data (:merge fns) (:post-merge fns))))



(defn get-records [plots]
  (map (fn [[k v]]
         (get-records-for-plot (:set v) (:set-options v) (keyword (:render v)) (keyword (:offset v)))) plots))






