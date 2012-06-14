(ns record-retriever
  
   (:require
    [mongo]
    ;[clojure.contrib.math :as math]
    ;[app.config :as config]
    ;[app.util.io :as io]
    [clojure.data.csv :as csv]
    [clojure.tools.logging :as lg]))



(mongo/connect "analytics")
(declare filter-point)
(declare get-day)
(defn now [] (java.util.Date.))
(defn log [msg]
  (println (now) msg))


(defn- receive-each [item all filter-fn]
   (if (or (empty?(:group all))
          (and (not= item nil) 
               (= (:user-id item) (:user-id (first (:group all))))))
    (update-in all [:group] conj (filter-point item))
        (assoc all :final (concat (:final all) 
        (filter-fn (sort-by :ts (:group all)))) :group [(filter-point item)])))

(defn- get-balance [accounts]
  (reduce + (map :balance accounts)))


(defn- filter-point 
  ([point offset] 
   ;(println "fp")
   ;(println point)
   ;(println "pt ts : " (:ts point))
    (when (and (:ts point) (not= (:ts point) 0))
      (let [
            balance (get-balance (:accounts point)) 
            ts (- (get-day (:ts point)) offset)]
        {:user-id (:user-id point) :ts ts :balance balance})))
  ([point] (filter-point point 0)))


(defn- balances-to-deltas [points]
  ;(log "btp")
  ;(log (str "btp "  (count points)))

  
  (let [balances (map :balance points) start (now)
      deltas (map (fn [a b]
                    (cond 
                      (nil? a) (log "a is nil")
                      (nil? b) (log "b is nil")
                      )
                    (- b a)) balances (rest balances))]
    (let [x (cons 
      (first points) 
      (map (fn [orig diff] 
          (assoc orig :balance diff)) (rest points) deltas)) ]
      ;(log (str "post btd map" (count points)))
                 x)))

(defn- balances-to-percent-change [points]
  (let [average (/ (reduce + (map :balance points)) (count points))]
    (map (fn [point] (update-in point [:balance] / average)) points)))


(defn- get-day [ts]
  (let [ts 
        (if (> ts 1000000000000)
          (/ ts 1000)
          ts)]
    (* (int (Math/floor (float (/ ts (* 60 60 24))))) 60 60 24 1000)))


(defn- merge-by-total [points]
  ;(log (str "mbt start" (count points)))
 ; (log (first points))
  ;(println (count final))
  ;(println (count point))
  (reduce (fn [a b]
            ;(log (str "a: " a " b: " b))
            [(:ts b) (+ (:balance b) (last a))]) [0 0] points))



(defn- post-merge-by-total [final point]
 ;(println "post mbt")
  (let [fin (if (nil? final) [] final) prev-bal (if (nil? final) 0 (last (last final)))]
    (conj fin [(first point) (+ prev-bal (double (/ (Math/round (float (* (last point) 100))) 100)))])))



(defn- merge-by-average [final point]
  (cond
    (empty? final) [{:count 1 :point (assoc point :ts (:ts point))}]
    (= (:ts (:point (last final))) (:ts point))
      (let [last-pos (dec (count final))]
        (update-in (update-in [last-pos :count] inc) [last-pos :point :balance] + (:balance (:point (last final)))))
   :else (conj final {:count 1 :point (assoc point :ts (:ts point))})))


(defn- post-merge-by-average [point]
  (log "post merge")
  (assoc (:point point) :balance (/ (:balance (:point point)) (:count point))))


(defn- merge-data [data merge-fn post-merge-fn]
  ;(println "md")
  (log (count data))
  (let [data (sort-by :ts data)
    l (log "merge-data")
    re (reduce post-merge-fn nil (pmap merge-fn (partition-by :ts data))
               )]
    (log "end")
    re))                
  
(def render-styles {
  :total {:filter balances-to-deltas :merge merge-by-total :post-merge post-merge-by-total}
  :average {:filter balances-to-percent-change :merge merge-by-average :post-merge post-merge-by-average}})



(defn- run-query [user-ids]
  (time (seq
  (mongo/get-cursor "filteredbalance" {:where {:user-id user-ids}  :sort {:user-id 1} :only {:user-id 1 :ts 1 :balance 1}}))))




(defn- get-records-lazy [start limit user-id]
  (let [stop (if (> limit 1000) 1000 limit)]
    (take-while (fn [a] (not= a nil)) (mapcat (fn [start] (run-query start stop user-id)) (range start limit 1000) )))) 



(defn get-all [render]
  (let [fns (render render-styles) 
        data (mapcat (:filter fns) (partition-by :user-id (filter (fn [a] (not= a nil)) (map filter-point (get-records-lazy 0 1000000)))))]
    (merge-data data (:merge fns) (:post-merge fns))))



(defn get-using-user-ids [render]
  (let [fns (render render-styles)
        user-ids (mongo/get-distinct "balance" "user-id")
        total-users (count user-ids)
        
        data (mapcat (fn [p] 
                       (println "wtf " (count p)) ((:filter fns) p)) (partition-by :user-id (filter (fn [a] (not= a nil) ) (map filter-point (map (fn [start]
               (let [end (if (> (+ start 20) total-users) total-users (+ start 20))
                     data (run-query (subvec user-ids start end))]
                 ;(println "start " start " end " end " count " (count data))
                 data)) (range 0 total-users 20))))))]  

          (println "total points " (count data))
    (merge-data data (:merge fns) (:post-merge fns))))




(defn get-using-user-ids [render]
  (let [fns (render render-styles)
        user-ids (mongo/get-distinct "balance" "user-id")
        total-users (count user-ids)

        data (mapcat (fn [pts] 
                  (mapcat (:filter fns) (partition-by :user-id (filter (fn [a] (not= (:balance a) nil)) pts))))
                    
                  (map (fn [start]
               (let [end (if (> (+ start 20) total-users) total-users (+ start 20))
                     data (run-query {"$in" (subvec user-ids start end)})]
                 ;(println "start " start " end " end " count " (count data))
                 data)) (range 0 total-users 20)))]
    (merge-data data (:merge fns) (:post-merge fns))))


(defn get-using-user-ids-batch [render]
  (let [fns (render render-styles)
        user-ids (mongo/get-distinct "balance" "user-id")
        total-users (count user-ids)
        data (apply concat (pmap (fn [pts]
                  (apply concat (pmap (:filter fns) (partition-by :user-id pts))))
                  (pmap (fn [start]
               (let [end (if (> (+ start 40) total-users) total-users (+ start 40))
                     ids (subvec user-ids start end)
                     data (run-query  {"$in" ids})]
                    (log ids)
                 data)) (range 0 total-users 40))))]
    (merge-data data (:merge fns) (:post-merge fns))))




(defn get-using-user-ids [render]
  (let [fns (render render-styles)
        user-ids (mongo/get-distinct "balance" "user-id")
        data (mapcat (fn [pts] ((:filter fns) (filter (fn [a] 
                                                        (if (nil? (:balance a)) (log a))
                                                        true)
                                                      ;(not= (:balance a) nil)
                                                       pts)))
                    
                  (map (fn [user-id] (run-query user-id)) user-ids))]
    (merge-data data (:merge fns) (:post-merge fns))))


(defn filter-to-mongo []
  (let [user-ids (mongo/get-distinct "balance" "user-id")]
        (map (fn [pts] 
                  (mongo/insert :filteredbalance (filter (fn [a] (not= a nil)) (map filter-point pts))))
                    
                  (map (fn [user-id] (run-query user-id)) user-ids))
    ))
