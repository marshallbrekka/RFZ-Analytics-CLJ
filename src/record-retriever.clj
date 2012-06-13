(ns record-retriever)

(defn- receive-each [item all filter-fn]
  (if (or 
        (empty(:group all)) 
        (= (:user-id item) (:user-id ((:group all) 0))))
   (update-in all :group conj item)
   (assoc all :final (conj (:final all) 
        (filter-fn (sort-by :ts (:group all)))) :group '(item))))

(defn- get-balance [accounts]
  (reduce + (map :balance accounts)))


(defn- filter-point 
  ([point offset] 
    (if (= (:ts point) nil) nil
      (let [
            balance (get-balance (:accounts point)) 
            ts (- (:ts point) offset)]
        (assoc point :ts ts :balance balance))))
  ([point] (filter-point point 0)))


(defn- balances-to-deltas [points]
  (let [balances (map :balance points)
      deltas (map #(- %2 %1) balances (rest balances))]
    (cons 
      (first points) 
      (map (fn [orig diff] 
          (assoc orig :balance diff)) points deltas))))

;(defn _balances-to-percent-change [points]
  

  


