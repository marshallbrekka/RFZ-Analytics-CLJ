(ns record-retriever.disc
  (:require
    [file-io]
    [record-retriever.internal :as internal]
    [mongo]))


(defn now [] (java.util.Date.))
(defn log [msg]
  (println (now) msg))
(def mongo-collection "snapshot-balances")
(def conn (mongo/connect "mydb"))
(def file-name "data2.json")
(def user-data nil)

(defn filter-point [point] 
    (when (and (:ts point) (not= (:ts point) 0))
      (merge {:ts-day (internal/get-day (:ts point))} point)))




(defn run-query [user-ids sort-using]
  ;(time (seq
  (mongo/run-query conn mongo-collection 
                   {:where {:user-id user-ids :active true :itemType {"$in" ["credits" "loans"]} }
                    :sort sort-using 
                    :only {:user-id 1 :ts 1 :balance 1 :account-id 1}}))




;;;;; new method for calculating deltas for accounts

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


(defn prep-user-points [pts]
  (log (str "# of points " (count pts) ". for user " (:user-id (first pts))))
  (let [filtered (filter internal/filter-nil (map filter-point pts))]
  {(keyword (str (:user-id (first pts)))) 
  (vec (calc-totals-from-deltas (merge-accounts (conj-account-timelines (map calc-deltas (partition-by :account-id filtered))))))}))






(defn serialize-from-mongo []
  (let [user-ids (mongo/get-distinct conn mongo-collection "user-id")
        total-users (count user-ids)
        l (log (str "total users " total-users))   
        f (file-io/open-write file-name)]
        (doseq [x (map (fn [uid] ;(log uid)
                                      (run-query uid {:account-id 1}))
                                    user-ids)]
                           (file-io/write-line f (prep-user-points x)))
        ;(log data)    
        ;(log (str "rows to write " (count data)))
        ;(log (count (last (first data))))
        ;(log (type data))
        ;(file-io/write-lines f data)
        (log "done")
        (file-io/close f)))



(defn deserialize-from-disc []
  (if (nil? user-data)
    (let [ud 
      (do
        (log "start")
        (let [f (file-io/open file-name)
         data (file-io/read-lines f)]
      (log "finished")
      (log (count data))
      data))]
     (def user-data ud)
     ud)
   user-data)) 
      


