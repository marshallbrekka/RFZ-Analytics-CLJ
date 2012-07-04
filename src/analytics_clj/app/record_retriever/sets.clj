(ns analytics-clj.app.record-retriever.sets
  (:require [cheshire.core :as json]
            [clj-http.client :as client])
  (:use 
            [clj-time.format]
            [clj-time.coerce]))
(def schema-url "https://beta.readyforzero.com/api/stat")

(defn create-schema-keys [schema]
  (cond (= (type schema) (type {}))
          (apply merge (map (fn [[a b]] {(keyword a) (create-schema-keys b)}) schema))
        (= (type schema) (type []))
          (vec (map create-schema-keys schema))
        :else
          schema))

    

(def schema (update-in (create-schema-keys (json/parse-string (:body (client/get (str schema-url "/json"))))) [:endpoints] 
                                          (fn [eps] (filter (fn [ep] (not= "/json" (last (:route ep)))) eps))))

(defn get-endpoint-helper[endpoints path]
  (if (empty? endpoints) nil
    (let [cur (first endpoints)]
      (if (= (last (:route cur)) path)
        cur
        (get-endpoint-helper (rest endpoints) path)))))


(defn get-endpoint [path]
  (get-endpoint-helper (:endpoints schema) path))
  



(declare make-sub-fields)
(defn get-json-spec []
  {:type   "select-sub-fields"
   :caption "User Set"
   :name    "set"
   :options (apply merge (map (fn [endpoint] {(last (:route endpoint)) (:info endpoint)}) (:endpoints schema)))
   :fields (apply merge (map (fn [endpoint] {(last (:route endpoint)) (make-sub-fields (:input-form endpoint))}) (:endpoints schema)))
  });

(defn make-sub-fields [input-form]
  (filter (fn [v] (not= v nil)) (map (fn [a b]

        (if (contains? (last a) :range) 
          (if (and (not= b nil) (:range (last b)))
            (let [mi (if (contains? (last a) :min) a b)
                  ma (if (= mi a) b a)]
              {:type  "range"
               :min   {:name (first mi) :value (:min (last mi)) :type (:type (last mi))}
               :max   {:name (first ma) :value (:max (last ma)) :type (:type (last ma))}
              })
            nil)
          {:type    (:type (last a))
           :caption (:label (last a))
           :name    (first a)
          })) input-form (conj (vec (rest input-form)) nil))))

(defn get-subset [params]
    (let [route (:set params)
          params (dissoc params :set)]
      (println route)
      (println params)

      (let [params (apply merge (map (fn [[k v]] {k (long (Float/parseFloat v))}) params))
        ids (json/parse-string (:body (client/post (str schema-url route) 
                                      {:headers {"Content-Type" "application/json" "Cookie" "disable-csrf=true;"} 
                                       :body (json/generate-string params)})))]
    (map (fn [id] (get id "id")) ids))))

(defn get-description [params]
  (let [route (:set params)
        params (dissoc params :set)
        data (get-endpoint route)
        filters (reduce 
                  (fn [re param]
                    (let [pval ((keyword (first param)) params)
                          pval (if (= (:type (last param)) "timestamp")
                                (unparse (formatter "MM/dd/yy")
                                    (from-long (long (Float/parseFloat pval))))
                                 pval)]
                      (str re (:label (last param)) ": " pval ". ")))
                    "" (:input-form data))]
        [(:info data) filters]))
        
    
    
(defn ids-to-keywords [ids]
  (map (fn [a] (keyword (str (int a)))) ids))

