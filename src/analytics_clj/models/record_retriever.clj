(ns record_retriever)


(defn _sort-by-ts [a b]
  (if (< (:ts a) (:ts b))
    -1
    (if (> (:ts a) (:ts b))
      1 0)))



(defn _recieve-each [item, all, filter-fn]
  (if (or 
        (= (count (:group all)) 0) 
        (= (:user-id item) (:user-id ((:group all) 0))))
   (assoc all :group (conj (:group all) item))
   (assoc all :final (conj (:final all) 
        (filter-fn (sort-by _sort-by-ts (:group all)))) :group '(item))))


(defn _filter-point 
  ([point offset] 
    (defn get-balance [accounts]
     (if (= (count accounts) 0) 
       0
      (reduce (fn [a b]
       (+ (:balance a) (:balance b))) accounts)))

    (if (= (:ts point) nil) nil
      (do 
      (def balance (get-balance (:accounts point)))
      (def ts (- (:ts point) offset))
      [(:user-id point) ts balance])))
  ([point] (_filter-point point 0)))


(defn _balances-to-deltas [points]
  (with-local-vars [prev 0, p []]
    (loop [i 0] 
      (when (< i 4)
        (def balance ((points i) 2))
          (var-set p (conj @p [((points i) 1) (- balance @prev)]))
          (var-set prev balance)
      (recur (inc i))))
    @p))

;(defn _balances-to-percent-change [points]
  

  


