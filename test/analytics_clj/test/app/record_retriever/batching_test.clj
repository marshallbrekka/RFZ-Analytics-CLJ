(ns analytics-clj.test.app.record-retriever.batching-test 
  (:require [analytics-clj.app.record-retriever.batching :as batching])
  (:use [clojure.test]))


(def input [[{:uid 1 :id 1 :points "x"}
             {:uid 1 :id 2 :points "y"}]
            [{:uid 2 :id 1 :points "t"}]])


(deftest seperate-users
  (testing "seperate-users"
    (let [output [{:info "UID 1"
                    :timelines '("x" "y")}
                   {:info "UID 2"
                    :timelines '("t")}]]
      (is (= (batching/seperate-users input) output)))))

(deftest seperate-accounts
  (testing "seperate-accounts"
    (let [output (list {:info "UID 1. AID 1"
                    :timelines '("x")}
                   {:info "UID 1. AID 2"
                    :timelines '("y")}
                   {:info "UID 2. AID 1"
                    :timelines '("t")})]
      (is (= (batching/seperate-accounts input) output)))))

(deftest merge-all 
  (testing "merge-all"
    (let [output [{:info "All"
                   :timelines (list "t" "x" "y")}]]
      (is (= (batching/merge-all input) output)))))


