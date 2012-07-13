(ns analytics-clj.app.record-retriever.disc
  (:require
    [analytics-clj.app.file-io :as file-io]
    [analytics-clj.app.record-retriever.internal :as internal]
    [analytics-clj.app.record-retriever.offset :as offset]
    [analytics-clj.app.mongo :as mongo]
    [analytics-clj.config :as config]))


(defn now [] (java.util.Date.))
(defn log [msg]
  (println (now) msg))
(defn logp [p]
  (do (println (now) p)
    p))
(def mongo-collection "snapshot-balances")
(def conn (apply mongo/connect (:mongo-balances config/conf)))

(declare types)

(defn filter-point [point] 
    (when (and (:ts point) (not= (:ts point) 0))
      (merge {:ts-day (internal/get-day (:ts point))} point)))




(defn run-query [user-ids sort-using]
  ;(time (seq
  (mongo/run-query conn 
                   mongo-collection 
                   {:where {:user-id user-ids 
                            :active true 
                            :itemType {"$in" ["credits"]}} ;"loans"]} }
                    :sort sort-using 
                    :only {:user-id 1 
                           :ts 1 
                           :balance 1 
                           :account-id 1 
                           :itemType 1}}))

(defn get-day-balances [pts]
  (->> pts (partition-by :ts-day) (map last)))

(defn calc-deltas 
  "takes a seq of points for a single account, not sorted by ts"
  [pts]
     (let [sorted (sort-by :ts pts)
          filtered (get-day-balances sorted)]
      (if (>= 1 (count filtered))
        filtered
          (apply conj [(first filtered)] (map (fn [a b] 
              (update-in b [:balance] (fn [b-balance]  (- b-balance (:balance a))))) filtered (rest filtered))))))

(defn conj-account-timelines [vecs]
  (reduce #(apply conj % %2) [] vecs))

(defn merge-day [pts]
  [(:ts-day (first pts)) 
   (-> (reduce #(+ % (:balance %2)) 0.0 pts)
       (* 100)
       (double)
       (Math/round)
       (/ 100)
       (double))])

(defn merge-accounts
  "takes a seq of deltas for various accounts 
   merges them by ts into deltas for each day"
  [pts]
  (->> pts
       (sort-by :ts-day)
       (partition-by :ts-day)
       (map merge-day)))
  

(defn calc-totals-from-deltas [deltas]
  (->> (reduce 
          #(conj % [(first %2) 
                    (+ (last (last %)) (last %2))]) 
          [(first deltas)] (rest deltas))
        (map #(update-in % [1] (fn [val] (-> val
                                           (double)
                                           (* 100)
                                           (Math/round)
                                           (/ 100)))))))
                                           
  



(defn get-first-day-from-accounts [accounts]
  (->> accounts (map #(:ts-day (first %)))
       (sort)
       (first)))
       

(defn extend-to-start-date [account-pts date]
  (map 
    (fn [account]
      (if (> (:ts-day (first account)) date)
          (cons 
            (update-in 
              (first account) 
              [:ts-day] 
              (fn [a] date)) 
            (cons 
              (update-in 
                (first account) 
                [:balance] 
                (fn [a] 0)) 
              (rest account)))
           account)) account-pts))

(defn match-start-date-on-accounts [accounts offsets]
  
  (->> (-> accounts
           (first)
           (first)
           (:user-id)
           (str)
           (keyword))
      (offset/get-offset offsets)
      (internal/get-day)
      (extend-to-start-date accounts)))


(defn merged-accounts [pts offsets]
  [{:type "all" 
    :points (->> pts 
            (map filter-point)
            (filter internal/filter-nil)
            (partition-by :account-id)
            (map calc-deltas)
            (#(match-start-date-on-accounts % offsets))
            (conj-account-timelines)
            (merge-accounts)
            (calc-totals-from-deltas)
            (vec))}])

(defn seperate-accounts [pts offsets]
  (->> pts
       (map filter-point)
       (filter internal/filter-nil)
       (partition-by :account-id)
       (map calc-deltas)
       (#(match-start-date-on-accounts % offsets))
       (#(map (fn [fullset totals]
                {:type (:itemType (first fullset))
                 :points totals
                 :id (:account-id (first fullset))})
              %
              (->> %
                   (map merge-accounts)
                   (map calc-totals-from-deltas)
                   (vec))))))



(defn prep-points [pts offsets type-key]
  ;(log pts)
  {(->> pts
          (first)
          (:user-id)
          (str)
          (keyword))
   ((:prep-fn (type-key types)) pts offsets)})


(def types {:merged   {:data (atom nil)
                       :file "merged.json"
                       :prep-fn merged-accounts}
            :seperate {:data (atom nil)
                       :file "seperate.json"
                       :prep-fn seperate-accounts}})


(defn serialize-from-mongo [type-key]
  (let [user-ids (mongo/get-distinct conn mongo-collection "user-id")
        offsets (offset/get-offsets "date-joined" user-ids)
        file (file-io/open-write (:file (type-key types)))]
        (doseq [x (map (fn [uid]
                         (log uid)
                         (run-query uid {:account-id 1}))
                       user-ids)]
                (file-io/write-line file (prep-points x offsets type-key)))
        (log "done")
        (file-io/close file)))



(defn deserialize-from-disc [type-key]
  (let [data-obj @(:data (type-key types))
        data (if (nil? data-obj)
                 (-> (type-key types)
                     (:file)
                     (file-io/open)
                     (file-io/read-lines)) 
                 data-obj)]
    (if (nil? data-obj)
        (reset! (:data (type-key types)) data))
    data))


       


