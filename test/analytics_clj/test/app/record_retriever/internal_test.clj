(ns analytics-clj.test.app.record-retriever.internal-test
  (:require [analytics-clj.app.record-retriever.internal :as internal]
            [analytics-clj.app.record-retriever.processing :as processing])
  (:use [clojure.test]))

(deftest get-day
  (testing "get-day"
    (let [input 1340048730
          output 1339977600000
          input2 1340048730000]
      (is (= (internal/get-day input) output))
      (is (= (internal/get-day input2) output)))))

(deftest filter-nil
  (testing "filter-nil"
    (let [input nil
          output false
          input2 "notnil"
          output2 true]
      (is (= (internal/filter-nil input) output))
      (is (= (internal/filter-nil input2) output2)))))


(deftest apply-offset
  (testing "apply-offset"
    (let [input [20 100]
          offset 0
          output [20 100]
          input2 [0 100]
          output2 nil
          input3 [nil 100]
          offset2 1341792000
          output3 [-1341791999980 100]
          offset3 -1341792000
          output4 [1341792000020 100]
          offset4 nil]
      (is (= (internal/apply-offset offset input) output))
      (is (= (internal/apply-offset offset input2) output2))
      (is (= (internal/apply-offset offset input3) output2))
      (is (= (internal/apply-offset offset2 input) output3))
      (is (= (internal/apply-offset offset3 input) output4))
      (is (= (internal/apply-offset offset4 input) output2))))) 

(deftest get-subset
  (testing "get-subset"
    (let [data {:1 [{:type "credits"
                      :points "x"}
                     {:type "loans"
                      :points "y"}]
                :2 [{:type "loans"
                      :points "t"}]
                :3 [{:type "credits"
                      :points "z"}]}
          input [[:1 :3] data]
          output {:3 (:3 data) :1 (:1 data)}
          input2 [[:1 :2 :3] data (fn [a] (= (:type a) "credits"))]
          output2 {:3 (list* (:3 data))  :1 (list (first (:1 data)))}]
     (is (= (apply internal/get-subset input) output))
     (is (= (apply internal/get-subset input2) output2)))))


(deftest put-to-days
  (testing "put-to-days"
    (let [data [[[0 10] [1 5] [2 -4]]
                [[0 3] [2 8]]]
          output {0 (list [0 3] [0 10]) 
                  1 (list [1 5])
                  2 (list [2 8] [2 -4])}
          real-out (internal/put-to-days data)]
      (dorun (map (fn [[k v]]
             (println k)
             (is (= @v (get output k))))
           real-out)))))

(deftest merge-data
  (testing "merge-data"
    (let [data [[[0 10] [1 5] [2 -4]]
                [[0 3] [2 8]]]
          merge-fn processing/merge-by-total
          post-fn processing/post-merge-by-total
          output [[0 13.0] [1 18.0] [2 22.0]]]
      (is (= (internal/merge-data data merge-fn post-fn) output)))))



