(ns analytics-clj.app.record-retriever.batching)

(defn seperate-users [users]
  (vec 
    (map (fn [timelines]
            {:info "users"
             :timelines (map :points timelines)}) 
         users)))

(defn seperate-accounts [users]
  (reduce #(apply conj % %2) [] 
    (map (fn [timelines]
            (map (fn [timeline]
                    {:info "accounts"
                     :timelines (list (:points timeline))}) timelines)) users)))


(defn extract-timelines [users]
  (map #(map :points %) users)) 


(defn merge-all [users]
  [{:info "all"
   :timelines (reduce #(apply conj % %2)
                      (extract-timelines users))}])


(def types {:accounts {:label "Accounts (debugging)"
                       :fn seperate-accounts
                       :accounts true}
            :users    {:label "Users"
                       :fn seperate-users
                       :accounts false}
            :merged   {:label "Flatten"
                       :fn merge-all
                       :accounts false}})


(defn batch [func-key users]
  (println "count users " (type users))
  ((:fn (func-key types)) users))
