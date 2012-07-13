(ns analytics-clj.test.app.record-retriever.batching-test 
  (:require [analytics-clj.app.record-retriever.batching :as batching])
  (:use [clojure.test]))


(def input [[{:points "x"}
             {:points "y"}]
            [{:points "t"}]])


(deftest seperate-users
  (testing "seperate-users"
    (let [output [{:info "users"
                    :timelines '("x" "y")}
                   {:info "users"
                    :timelines '("t")}]]
      (is (= (batching/seperate-users input) output)))))

(deftest seperate-accounts
  (testing "seperate-accounts"
    (let [output (list {:info "accounts"
                    :timelines '("x")}
                   {:info "accounts"
                    :timelines '("y")}
                   {:info "accounts"
                    :timelines '("t")})]
      (is (= (batching/seperate-accounts input) output)))))

(deftest merge-all 
  (testing "merge-all"
    (let [output [{:info "all"
                   :timelines (list "t" "x" "y")}]]
      (is (= (batching/merge-all input) output)))))


