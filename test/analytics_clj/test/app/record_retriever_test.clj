(ns analytics-clj.test.app.record-retriever-test
  (:require [analytics-clj.app.record-retriever :as rr]
            [analytics-clj.app.record-retriever.processing :as processing])
  (:use [clojure.test]))

(deftest filter-timelines
  (testing "filter-timelines"
    (let [timelines [{:points [[1 100] [2 20] [3 40]]} {:points [[1 20] [2 10] [3 5]]}]
          filter-fn (fn [timeline] (map (fn [pt] [(first pt) (+ 1 (last pt))]) 
                                        timeline))
          offset 0
          output (list {:points (list [1 101] [2 21] [3 41])} {:points (list [1 21] [2 11] [3 6])})
          offset2 1341792000
          output2 (list {:points (list[-1341791999999 101] [-1341791999998 21] [-1341791999997 41])} {:points (list [-1341791999999 21] [-1341791999998 11] [-1341791999997 6])})
          offset3 nil
          output3 (list {:points (list)} {:points (list)}) ]
      (is (= (rr/filter-timelines filter-fn offset timelines) output))
      (is (= (rr/filter-timelines filter-fn offset2 timelines) output2))
      (is (= (rr/filter-timelines filter-fn offset3 timelines) output3)))))

(deftest merge-batches
  (testing "merge-batches"
    (let [batches [{:info "info"
                    :timelines [[[0 10] [1 20] [2 30]] 
                                [[0 5] [1 25] [2 5]]]}]
          merge-fn processing/merge-by-total
          post-merge processing/post-merge-by-total
          output (list {:info "info"
                        :timelines [[0 15.0] [1 60.0] [2 95.0]]})]
      (is (= (rr/merge-batches merge-fn post-merge batches) output)))))

(deftest filter-out-empty-timelines
  (testing "filter-out-empty-timelines"
    (let [input [[{:points '()}
                  {:points '()}]
                 [{:points (list 1)}
                  {:points '()}]]
          output (list (list {:points (list 1)}))]
      (is (= (rr/filter-out-empty-timelines input) output)))))

(deftest process-plot-data
  (testing "process-plot-data"
    (let [pts {:1 [{:points [[1 100] [2 20] [3 40]]} {:points [[1 20] [2 10] [3 5]]}]
               :2 [{:points [[1 100] [2 20] [3 40]]}]}
          offsets {:1 0}
          batch-type :merged
          fns (:total processing/graph-types)
          output (list {:info "all"
                        :timelines [[1 120.0] [2 30.0] [3 45.0]]})]
      (is (= (rr/process-plot-data pts offsets batch-type fns)) output))))
          
          

