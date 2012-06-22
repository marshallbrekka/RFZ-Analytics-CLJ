(ns record-retriever.processing-test
  (:require [record-retriever.processing :as pr])
  (:use [clojure.test]))

(deftest balances-to-deltas
   (testing "balances-to-deltas" 
     (let [input [[0 1] [1 5] [6 4.5]]
           output '([0 1] [1 4] [6 -0.5])]
       (is (= (pr/balances-to-deltas input) output)))))


(deftest merge-by-total
    (testing "merge-by-total"
      (let [input [[0 10] [0 15] [0 -11.6]]
            output [0 13.4]
            input2 [[1 10] [1 -4]]
            output2 [1 6]]
        (is (= (pr/merge-by-total input) output))
        (is (= (pr/merge-by-total input2) output2)))))

(deftest post-merge-by-total
    (testing "post-merge-by-total"
      (let [input [nil [2 10]]
            output [[2 10.0]]
            input2 [[[0 10.0] [2 13.5]] [3 -3]]
            output2 [[0 10.0] [2 13.5] [3 10.5]]]
        (is (= (apply pr/post-merge-by-total input) output))
        (is (= (apply pr/post-merge-by-total input2) output2)))))


(deftest balances-to-percent-change
   (testing "balances-to-percent-change"
      (let [input [[0 2] [1 4] [6 6] [7 4]]
            output '([0 1/2] [1 1] [6 3/2] [7 1])
            input2 [[0 0] [2 0]]
            output2 '([0 0] [2 0])]
        (is (= (pr/balances-to-percent-change input) output))
        (is (= (pr/balances-to-percent-change input2) output2)))))

(deftest merge-by-average
    (testing "merge-by-average"
      (let [input [[0 5]]
            output {:count 1 :point [0 5]}
            input2 [[2 7] [2 7] [2 3]]
            output2 {:count 3 :point [2 17]}]
        (is (= (pr/merge-by-average input) output))
        (is (= (pr/merge-by-average input2) output2)))))


(deftest post-merge-by-average
    (testing "post-merge-by-average"
      (let [input [[] {:count 2 :point [2 4]}]
            output [[2 2]]]
        (is (= (apply pr/post-merge-by-average input) output)))))


