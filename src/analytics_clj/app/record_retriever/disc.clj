(ns analytics-clj.app.record-retriever.disc
  (:require
    [analytics-clj.app.file-io :as file-io]
    [analytics-clj.app.record-retriever.internal :as internal]
    [analytics-clj.app.record-retriever.offset :as offset]
    [analytics-clj.app.mongo :as mongo]))


(defn now [] (java.util.Date.))
(defn log [msg]
  (println (now) msg))
(def mongo-collection "snapshot-balances")
(def conn (mongo/connect "mydb"))
(def file-name "data2.json")
(def file-name-seperate "data-seperate.json")

(def user-data nil)
(def user-data-flat nil)

(defn filter-point [point] 
    (when (and (:ts point) (not= (:ts point) 0))
      (merge {:ts-day (internal/get-day (:ts point))} point)))




(defn run-query [user-ids sort-using]
  ;(time (seq
  (mongo/run-query conn mongo-collection 
                   {:where {:user-id user-ids :active true :itemType {"$in" ["credits"]}} ;"loans"]} }
                    :sort sort-using 
                    :only {:user-id 1 :ts 1 :balance 1 :account-id 1 :itemType 1}}))

(defn get-day-balances [pts]
  (map (fn [logs] (last logs)) (partition-by :ts-day pts)))

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
  (reduce (fn [a b] (apply conj a b)) [] vecs))

(defn merge-day [pts]
  (let [balance (reduce (fn [a b] (+ a (:balance b))) 0.0 pts)]
    [(:ts-day (first pts)) (double (/ (Math/round (double (* balance 100))) 100))]))

(defn merge-accounts
  "takes a seq of deltas for various accounts 
   merges them by ts into deltas for each day"
  [pts]
  (let [sorted (sort-by :ts-day pts)]
    (map merge-day (partition-by :ts-day sorted)))) 


(defn calc-totals-from-deltas [deltas]
  (reduce (fn [a b] (conj a [(first b) (+ (last (last a)) (last b))])) [(first deltas)] (rest deltas)))



(defn get-first-day-from-accounts [accounts]
  (first (sort (map #(:ts-day (first %)) accounts))))


(defn extend-to-start-date [date account-pts]
  (map (fn [account]
    (if (> (:ts-day (first account)) date)
      (cons (update-in (first account) [:ts-day] (fn [a] date)) (cons (update-in (first account) [:balance] (fn [a] 0)) (rest account)))
      account)) account-pts))

(defn match-start-date-on-accounts [accounts offsets]
  ;(log accounts)
  ;(-> (get-first-day-from-accounts accounts) 
  (extend-to-start-date (internal/get-day (offset/get-offset offsets (keyword (str (:user-id (first (first accounts))))))) accounts ))




(defn prep-user-points [pts offsets]
  ;(log (str "# of points " (count pts) ". for user " (:user-id (first pts))))
  (let [filtered (filter internal/filter-nil (map filter-point pts))]
  {(keyword (str (:user-id (first pts)))) [{:type "all" :points
    (->> (match-start-date-on-accounts (map calc-deltas (partition-by :account-id filtered)) offsets)
         (conj-account-timelines)
         (merge-accounts)
         (calc-totals-from-deltas)
         (vec))}]}))
   ;(vec (calc-totals-from-deltas (merge-accounts (conj-account-timelines (map calc-deltas (partition-by :account-id filtered))))))}))



(defn prep-user-points-seperate-accounts [pts offsets]
  ;(log (str "# of points " (count pts) ". for user " (:user-id (first pts))))
  (let [filtered (filter internal/filter-nil (map filter-point pts))
        partitioned (partition-by :account-id filtered)
        deltas (match-start-date-on-accounts (map calc-deltas partitioned) offsets)
        ;l (log deltas)
        merged-accounts (map merge-accounts deltas)
        ;l (log merged-accounts)
        totals-from-deltas (map calc-totals-from-deltas merged-accounts)
        ;l (log totals-from-deltas)
        ]
  {(keyword (str (:user-id (first pts))))
  (map (fn [fullset totals] {:type (:itemType (first fullset)) :points totals}) deltas (vec totals-from-deltas))}))




(defn serialize-from-mongo [flat-accounts?]
  (println flat-accounts?)
  (let [prep-fn (if flat-accounts? prep-user-points prep-user-points-seperate-accounts)
        file (if flat-accounts? file-name file-name-seperate)
        user-ids (mongo/get-distinct conn mongo-collection "user-id")
        offsets (offset/get-offsets "date-joined" user-ids)

        total-users (count user-ids)
        l (log (str "total users " total-users))   
        f (file-io/open-write file)]
        (doseq [x (map (fn [uid] (log uid)
                                      (run-query uid {:account-id 1}))
                                    user-ids)]
                           (file-io/write-line f (prep-fn x offsets)))
        ;(log data)    
        ;(log (str "rows to write " (count data)))
        ;(log (count (last (first data))))
        ;(log (type data))
        ;(file-io/write-lines f data)
        (log "done")
        (file-io/close f)))



(defn deserialize-from-disc [flat-file?]
  (let [data-obj (if flat-file? user-data-flat user-data)
        ud (if (nil? data-obj)
            (do
            (log "start")
            (let [file (if flat-file? file-name file-name-seperate)
                  f (file-io/open file)
                  data (file-io/read-lines f)]
              (log "finished")
              (log (count data))
              (log (type data))
            data)) data-obj)]
      (if flat-file?
      (def user-data-flat ud)
      (def user-data ud))
    ud)) 
      


