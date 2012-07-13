(ns analytics-clj.test.app.record-retriever.disc-test
  (:require [analytics-clj.app.record-retriever.disc :as disc])
  (:use [clojure.test]))

(deftest get-day-balances 
  (testing "get-day-balances"
    (let [input [{:ts 99 :ts-day 0 :account-id 0 :balance 20} {:ts 100 :ts-day 0 :account-id 0 :balance 18} {:ts 200 :ts-day 1 :account-id 0 :balance 2}]
          output [{:ts 100 :ts-day 0 :account-id 0 :balance 18} {:ts 200 :ts-day 1 :account-id 0 :balance 2}]]
      (is (= (disc/get-day-balances input) output)))))

(deftest calc-deltas
  (testing "calc-deltas"
    (let [input [{:ts 99 :ts-day 0 :account-id 0 :balance 20} {:ts 100 :ts-day 0 :account-id 0 :balance 18} {:ts 200 :ts-day 1 :account-id 0 :balance 2}]
          output [{:ts 100 :ts-day 0 :account-id 0 :balance 18} {:ts 200 :ts-day 1 :account-id 0 :balance -16}]
          input2 [{:ts 99 :ts-day 0 :account-id 0 :balance 20} {:ts 100 :ts-day 1 :account-id 0 :balance 20} {:ts 200 :ts-day 2 :account-id 0 :balance 2}]
          output2 [{:ts 99 :ts-day 0 :account-id 0 :balance 20} {:ts 100 :ts-day 1 :account-id 0 :balance 0} {:ts 200 :ts-day 2 :account-id 0 :balance -18}]
          input3 [{:ts 99 :ts-day 0 :account-id 0 :balance 20}]
          output3 [{:ts 99 :ts-day 0 :account-id 0 :balance 20}]
          input4 []
          output4 []]
      (is (= (disc/calc-deltas input) output))
      (is (= (disc/calc-deltas input2) output2))
      (is (= (disc/calc-deltas input3) output3))
      (is (= (disc/calc-deltas input4) output4)))))

(deftest merge-day
  (testing "merge-day"
    (let [input [{:ts 99 :ts-day 0 :account-id 0 :balance 20} {:ts 100 :ts-day 0 :account-id 0 :balance 18} {:ts 100 :ts-day 0 :account-id 0 :balance -4}]
          output [0 34.0]]
     (is (= (disc/merge-day input) output))))) 

(deftest merge-accounts
  (testing "merge-accounts"
    (let [input [{:user-id 0 :account-id 0 :ts 0 :ts-day 0 :balance 10} 
                 {:user-id 0 :account-id 0 :ts 100 :ts-day 1 :balance 20} 
                 {:user-id 0 :account-id 1 :ts 100 :ts-day 1 :balance -5}
                 {:user-id 0 :account-id 1 :ts 0 :ts-day 0 :balance 20}]
          output '([0 30.0] [1 15.0])]
      (is (= (disc/merge-accounts input) output)))))


(deftest calc-totals-from-deltas
  (testing "calc-totals-from-deltas"
    (let [input [[0 10] [1 20]  [2 -4]]
          output [[0 10] [1 30] [2 26]]]
      (is (= (disc/calc-totals-from-deltas input) output)))))
          

(deftest extend-to-start-date
  (testing "extend-to-start-date"
    (let [input [[{:ts-day 10  :balance 10}] 
                 [{:ts-day 20 :balance 13}]
                 [{:ts-day 0 :balance 3}
                  {:ts-day 4 :balance 5}]]
          start-date 0
          output [[{:ts-day 0  :balance 10}
                   {:ts-day 10  :balance 0}] 
                  [{:ts-day 0 :balance 13}
                   {:ts-day 20 :balance 0}]
                  [{:ts-day 0 :balance 3}
                   {:ts-day 4 :balance 5}]]]
      (is (= (disc/extend-to-start-date input start-date) output)))))
      
(deftest get-first-day-from-accounts
  (testing "get-first-day-from-accounts"
    (let [input [[{:ts-day 10  :balance 10}] 
                 [{:ts-day 20 :balance 13}]
                 [{:ts-day 0 :balance 3}
                  {:ts-day 4 :balance 5}]]
          output 0]
      (is (= (disc/get-first-day-from-accounts input)) output))))


(deftest merged-accounts
  (testing "merged-accounts"
    (let [input [{:user-id 0 :account-id 0 :ts 1326603842001 :balance 20 :itemType "credits"} 
                 {:user-id 0 :account-id 0 :ts 1326603842000 :balance 100 :itemType "credits"} 
                 {:user-id 0 :account-id 0 :ts 1339358018251 :balance 40 :itemType "credits"}
                 {:user-id 0 :account-id 1 :ts 1326603842000 :balance 20 :itemType "credits"} 
                 {:user-id 0 :account-id 1 :ts 1339391042000 :balance 10 :itemType "credits"}]
          offsets {:0 1296703848847, :3 1296633600000, :1 1296633600000}
          output [{:type "all" :points [[1296691200000 40] [1326585600000 40] [1339286400000 60] [1339372800000 50]]}]
          input2 [{:user-id 0 :account-id 0 :ts 1326603842001 :balance 20 :itemType "credits"} 
                 {:user-id 0 :account-id 0 :ts 1326603842000 :balance 100 :itemType "credits"} `
                 {:user-id 0 :account-id 0 :ts 1339358018251 :balance 40 :itemType "credits"}
                 {:user-id 0 :account-id 1 :ts 1339391042000 :balance 10 :itemType "credits"}]
                output2 [{:type "all" :points [[1296691200000 30] [1326585600000 30] [1339286400000 50] [1339372800000 50]]}]]
        (is (= (disc/merged-accounts input offsets) output))
        (is (= (disc/merged-accounts input2 offsets) output2)))))
      

(deftest seperate-accounts
  (testing "seperate-accounts"
    (let [input [{:user-id 0 :account-id 0 :ts 1326603842001 :balance 20 :itemType "credits"} 
                 {:user-id 0 :account-id 0 :ts 1326603842000 :balance 100 :itemType "credits"} 
                 {:user-id 0 :account-id 0 :ts 1339358018251 :balance 40 :itemType "credits"}
                 {:user-id 0 :account-id 1 :ts 1326603842000 :balance 20 :itemType "credits"} 
                 {:user-id 0 :account-id 1 :ts 1339391042000 :balance 10 :itemType "credits"}]
          offsets {:0 1296703848847, :3 1296633600000, :1 1296633600000}
          output [{:type "credits" :id 0 :points (list [1296691200000 20] [1326585600000 20] [1339286400000 40])} {:type "credits" :id 1 :points (list [1296691200000 20] [1326585600000 20] [1339372800000 10])}]
          input2 [{:user-id 0 :account-id 0 :ts 1326603842000 :balance 10 :itemType "credits"}
                  {:user-id 0 :account-id 1 :ts 1339391042000 :balance 20 :itemType "credits"}]
          output2 [{:type "credits" :id 0 :points (list [1296691200000 10] [1326585600000 10])} {:type "credits" :id 1 :points (list [1296691200000 20] [1339372800000 20])}]]

      (is (= (disc/seperate-accounts input offsets) output))
      (is (= (disc/seperate-accounts input2 offsets) output2)))))


