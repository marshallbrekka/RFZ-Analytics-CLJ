(ns record-retriever.internal-test
  (:require [record-retriever.internal :as internal])
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
      (is (= (disc/filter-nil input) output))
      (is (= (disc/filter-nil input2) output2)))))


(deftest filter-point 
  (testing "filter-point"
    (let [input {:ts 1332036360000 :balance 100}
          output {:ts 1332028800000 :balance 100}
          input2 {:ts 0 :balance 100}
          output2 nil
          input3 [{:ts 1340048730000 :balance 10} 1332028800000]
          output3 {:ts 8019930000 :balance 10}]
      (is (= (internal/filter-point input) output))
      (is (= (internal/filter-point input2) output2))
      (is (= (apply internal/filter-point input3) output3)))))


