(ns analytics-clj.app.record-retriever.processing)


(defn balances-to-deltas [points]
  (let [deltas (map 
                 (fn [[a-ts a-balance] [b-ts b-balance]]
                    [b-ts (- b-balance a-balance)]) points (rest points))]
     (cons (first points) deltas)))       

(defn merge-by-total [points]
  (reduce 
    (fn [a b]
      [(first b) 
       (-> b
            (last)
            (* 100)
            (int)
            (/ 100)
            (double)
            (+ (last a)))]) 
    [0 0] points))

(defn post-merge-by-total [final point]
  (let [fin (if (nil? final) 
                [] 
                final)
        prev-bal (if (nil? final) 
                      0 
                      (* 100 (last (last final))))]
    (conj fin [(first point) (double (/ (+ prev-bal (* 100 (last point))) 100))])))

(defn balances-to-percent-change [points]
  (if (empty? points)
      points
      (let [average (->> points
                         (map last)
                         (reduce +)
                         (#(/ % (count points))))  
            update-fn (if (= (double average) 0.0) 
                          (fn [a] 0) 
                          (fn [a] (- (/ a average) 1)))]
        (balances-to-deltas (map #(update-in % [1] update-fn) points)))))

"(defn merge-by-average [points]
  (let [sum  (reduce #(+ % (last %2)) 0 points)]
    {:count (count points) 
     :point [(first (first points)) sum]}))
   
(defn post-merge-by-average [final point]
  (let [fin (if (nil? final) [] final)]
      (conj fin [(first (:point point))  (/ (last (:point point)) (:count point)) ])))"




(defn balances-to-percentage-of-start [points]
  (let [start (->> points
                  (first)
                  (last)
                  (#(if (= 0.0 (double %)) 1 %)))]
    (balances-to-deltas (map (fn [[t b]] [t (/ b start)]) points))))
    

(defn merge-by-start [points]
  (reduce (fn [a b]
      [(first b) (+ (* 100 (last b)) (last a))]) [0 0] points))




(def graph-types {
  :total {:filter balances-to-deltas :merge merge-by-total :post-merge post-merge-by-total :label "Total Balances"}
  :average {:filter balances-to-percent-change :merge merge-by-total :post-merge post-merge-by-total :label "Mean-Balance Rescaling"}
  :average-from-start {:filter balances-to-percentage-of-start :merge merge-by-start :post-merge post-merge-by-total :label "Percent Change from Start"}})



(defn get-json-spec []
  {:name    "render" 
   :type    "select" 
   :caption "Render Mode" 
   :options (apply merge (map (fn [[k v]] {k (:label v)}) graph-types))
  })


  
(defn get-description [type-key]
  (println type-key)
  (str "Graph Type: " (:label (type-key graph-types))))
