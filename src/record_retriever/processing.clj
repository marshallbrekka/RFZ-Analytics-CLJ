(ns record-retriever.processing)




(defn balances-to-deltas [points]
  (let [deltas (map (fn [[a-ts a-balance] [b-ts b-balance]]
                    [b-ts (- b-balance a-balance)]) points (rest points))]
    (let [x (cons (first points) deltas)]
        x)))

(defn merge-by-total [points]
  (reduce (fn [a b]
      [(first b) (+ (int (* 100 (last b))) (last a))]) [0 0] points))

(defn post-merge-by-total [final point]
  (if (nil? point)
    (println "point is nil"))
  (let [fin (if (nil? final) [] final)
        prev-bal (if (nil? final) 0 (* 100 (last (last final))))]
    (conj fin [(first point) (/ (+ prev-bal (last point)) 100)])))

(defn balances-to-percent-change [points]
  (if (empty? points)
    points
  (let [average (/ (reduce + 
                          (map last points)) 
                  (count points))
        l (println "average " average)
        update-fn (if (= (double average) 0.0) (fn [a] 0) (fn [a] (/ a average)))]
    (map (fn [point] (update-in point [1] update-fn)) points))))

(defn merge-by-average [points]
  (let [sum  (reduce (fn [a b] (+ a (last b))) 0 points)]
    {:count (count points) :point [(first (first points)) sum]}))
   
(defn post-merge-by-average [final point]
  (let [fin (if (nil? final) [] final)]
      (conj fin [(first (:point point))  (/ (last (:point point)) (:count point))])))

(def graph-types {
  :total {:filter balances-to-deltas :merge merge-by-total :post-merge post-merge-by-total}
  :average {:filter balances-to-percent-change :merge merge-by-average :post-merge post-merge-by-average}})
