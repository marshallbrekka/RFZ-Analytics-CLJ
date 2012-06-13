(ns record-retriever
  
   (:require
    [mongo]
    [clojure.contrib.math :as math]
    ;[app.config :as config]
    ;[app.util.io :as io]
    [clojure.data.csv :as csv]
    [clojure.tools.logging :as lg]))



(mongo/connect "analytics")
(declare filter-point)
(declare get-day)


(defn- receive-each [item all filter-fn]
   (if (or (empty?(:group all))
          (and (not= item nil) 
               (= (:user-id item) (:user-id ((:group all) 0)))))
    (update-in all [:group] conj (filter-point item))
        (assoc all :final (concat (:final all) 
        (filter-fn (sort-by :ts (:group all)))) :group [(filter-point item)])))

(defn- get-balance [accounts]
  (reduce + (map :balance accounts)))


(defn- filter-point 
  ([point offset] 
    (if (or (= point nil) (= (:ts point) nil)) nil
      (let [
            balance (get-balance (:accounts point)) 
            ts (- (get-day (:ts point)) offset)]
        {:user-id (:user-id point) :ts ts :balance balance})))
  ([point] (filter-point point 0)))


(defn- balances-to-deltas [points]
  
  (let [balances (map :balance points)
      deltas (map #(- %2 %1) balances (rest balances))]
    (let [x (cons 
      (first points) 
      (map (fn [orig diff] 
          (assoc orig :balance diff)) (rest points) deltas)) ]
      (println "post btd map")
                 x)))

(defn- balances-to-percent-change [points]
  (let [average (/ (reduce + (map :balance points)) (count points))]
    (map (fn [point] (assoc point :balance (/ (:balance point) average))) points))) 

(defn- get-day [ts]
  (let [ts 
        (if (> ts 1000000000000)
          (/ ts 1000)
          ts)]
    (* (int (math/floor (float (/ ts (* 60 60 24))))) 60 60 24 1000)))


(defn- merge-by-total [final point]
  (println "mbt start")
  (cond 
    (empty? final) [(assoc point :ts (:ts point))]
    (= (:ts (last final)) (:ts point))
      (conj (subvec final 0 (- (count final) 1)) (assoc (last final) :balance (+ (:balance point) (:balance (last final)))))
    :else (conj final {:ts (:ts point) :balance (+ (:balance point) (:balance (last final)))})))

(defn- post-merge-by-total [point]
  (println "post mbt")
  point)

(defn- merge-by-average [final point]
  (cond
    (empty? final) [{:count 1 :point (assoc point :ts (:ts point))}]
    (= (:ts (:point (last final))) (:ts point))
      (conj (subvec final 0 (- (count final) 1)) (assoc (last final) :count (+ 1 (:count (last final))) :point (assoc (:point (last final)) :balance (+ (:balance point) (:balance (:point (last final)))))))
   :else (conj final {:count 1 :point (assoc point :ts (:ts point))})))

(defn- post-merge-by-average [point]
  (assoc (:point point) :balance (/ (:balance (:point point)) (:count point))))


(defn- merge-data [data merge-fn post-merge-fn]
  (let [data (sort-by :ts data)]
    (println "merge-data")
      (map (fn [data] 
          (let [d (post-merge-fn data)]
            [(:ts d) 
             (double (/ (math/round 
                          (float 
                            (* (:balance d) 100))) 
                        100)
                     )]
            ))(reduce merge-fn [] data))))
                
  
(def render-styles {
  :total {:filter balances-to-deltas :merge merge-by-total :post-merge post-merge-by-total}
  :average {:filter balances-to-percent-change :merge merge-by-average :post-merge post-merge-by-average}})

(defn- get-helper [cursor all render-key]
  (if (mongo/has-next? cursor)
    (get-helper cursor (receive-each (mongo/get-next cursor) all (:filter render-key)) render-key)
    (merge-data (:final (receive-each nil all (:filter render-key))) (:merge render-key) (:post-merge render-key))))

(defn get-records []
  (let [cursor (mongo/get-cursor "balances" {:limit 1000 :sort {:user-id 1} :only {:user-id 1 :ts 1 :accounts 1}})
        all {:group [] :final []}]
    (get-helper cursor all (:total render-styles))))




  

  


