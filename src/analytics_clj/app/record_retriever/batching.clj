(ns analytics-clj.app.record-retriever.batching)

(defn seperate-users [users]
  (vec 
    (map (fn [timelines]
            {:info (str "UID " (:uid (first timelines)))
             :timelines (map :points timelines)}) 
         users)))

(defn seperate-accounts [users]
  (reduce #(apply conj % %2) [] 
    (map (fn [timelines]
            (map (fn [timeline]
                    {:info (str "UID " (:uid timeline) ". AID " (:id timeline))
                     :timelines (list (:points timeline))}) timelines)) users)))


(defn extract-timelines [users]
  (map #(map :points %) users)) 


(defn merge-all [users]
  [{:info "All"
    :timelines (reduce (fn ([a b] (apply conj a b)) ([] '()))
                       (extract-timelines users))}])
(defn by-type [users]
  (let [types (atom {})]
    (doseq [timelines users]  
           (doseq [timeline timelines]
                 (if (contains? @types (:type timeline))
                      
                     (swap! (get @types (:type timeline)) 
                            conj 
                            (:points timeline))
                     (swap! types assoc (:type timeline) (atom (list (:points timeline)))))))
      (map (fn [[label value]]
            {:info (str "Acct Type: " label)
             :timelines @value}) @types)))
    
   

  


(def types {:accounts {:label "Accounts (debugging)"
                       :fn seperate-accounts
                       :accounts true}
            :users    {:label "Users"
                       :fn seperate-users
                       :accounts false}
            :merged   {:label "Flatten"
                       :fn merge-all
                       :accounts false}
            :by-type  {:label "By Account Type"
                       :fn by-type
                       :accounts true}})

(defn sort-map [key1 key2]
  (let [order [:merged :users :accounts :by-type]]
        (compare (.indexOf order key1)
                 (.indexOf order key2))))
(def type-order [:merged :by-type :users :accounts])


(defn batch [func-key users]
  (println "count users " (type users))
  ((:fn (func-key types)) users))

(defn get-json-spec []
  {:type   "select"
   :caption "Batching Method"
   :name    "batch"
   :options (into (sorted-map-by sort-map)
                  (apply merge (map 
                                 (fn [t] {t (:label (t types))})
                                 type-order)))})

