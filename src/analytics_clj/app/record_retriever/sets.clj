(ns analytics-clj.app.record-retriever.sets
  (:require [cheshire.core :as json]
            [clj-http.client :as client]
            [analytics-clj.app.file-io :as file-io]
            [analytics-clj.config      :as config])
  (:use 
            [clj-time.format]
            [clj-time.coerce]
            [clojure.set :only [intersection]]))
(def schema-url (:endpoint-url config/conf))
(def secret (file-io/read-first-line (file-io/open "secret.txt")))

(defn create-schema-keys [schema]
  (cond (map? schema)
          (apply merge (map (fn [[a b]] {(keyword a) (create-schema-keys b)}) schema))
        (vector? schema)
          (vec (map create-schema-keys schema))
        :else
          schema))

(def schema 
  (-> (str schema-url "/json?secret=" secret)
       (client/get)
       (:body)
       (json/parse-string)
       (create-schema-keys) 
       (update-in [:endpoints] 
          (fn [eps] 
            (filter 
              (fn [ep] 
                (not= "/json" 
                  (last (:route ep)))) 
              eps)))))

(defn get-endpoint [path]
  (->  #(= (last (:route %)) path)
       (filter (:endpoints schema))
       (first)))

(defn process-type [type-key]
  (if (= type-key "string")
      "input"
      type-key))

(defn make-sub-fields [input-form]
  (filter 
    (fn [v] (not= v nil)) 
      (map 
        (fn [a b]
          (if (contains? (last a) :range) 
            (if (and (not= b nil) (:range (last b)))
              (let [mi (if (contains? (last a) :min) a b)
                    ma (if (= mi a) b a)]
                {:type  "range"
                 :min   {:name (first mi) :value (:min (last mi)) :type (:type (last mi))}
                 :max   {:name (first ma) :value (:max (last ma)) :type (:type (last ma))}})
                nil)
            (-> {:type    (process-type (:type (last a)))
                 :caption (:label (last a))
                 :name    (first a)}
              (#(if (contains? (last a) :options)
                    (assoc % :options (:options (last a)))
                    %))))) 
        input-form (conj (vec (rest input-form)) nil))))



(defn get-json-spec []
  {:type   "select-sub-fields"
   :caption "User Set"
   :name    "set"
   :multiple true
   :options (apply merge (map 
                           (fn [endpoint] 
                             {(last (:route endpoint)) (:info endpoint)}) 
                           (:endpoints schema)))
   :fields (apply merge (map 
                          (fn [endpoint] 
                            {(last (:route endpoint)) (make-sub-fields (:input-form endpoint))}) 
                          (:endpoints schema)))})

(defn get-endpoint [route]
  (first (filter  #(= route (last (:route %))) (:endpoints schema))))

(defn process-param [route pkey pval]
  ;(println route pkey pval)
  ;(println (json/generate-string [route pkey pval]))
  (let [ptype (->> (get-endpoint route)
                   (:input-form)
                   (filter #(= pkey (first %)))
                   (first)
                   (last)
                   (:type))]
      (if (or (= ptype "number") (= ptype "timestamp"))
          (long (Float/parseFloat pval))
          pval)))

(defn get-subset [set-params]
  (println "set params!!!!!!!! " set-params)
    (let [route (:set set-params)
          params (->> (dissoc set-params :set)
                      (map
                       (fn [[k v]] 
                         {k (process-param route (name k) v)}))
                      (apply merge))
          ids (-> (str schema-url route)
                  (client/post {:headers {"Content-Type" "application/json" 
                                          "Cookie" "disable-csrf=true;"} 
                                :body (json/generate-string (assoc params :secret secret))})
                  (:body)
                  (json/parse-string))]
      (if (empty? ids)
          '()
          (map #(get % "id") ids))))

(defn get-combined-sets [sets]
  (println "sets!!!!!!!!!!!! "  sets)
  (->> (pmap (fn [[k v]] (get-subset v)) sets)
       (map set)
       (apply intersection)))
      

(defn get-description 
  "returns a pretty description of the set and its filter params"
  [set-params]
  (println set-params)
  (let [route (:set set-params)
        params (dissoc set-params :set)
        data (get-endpoint route)
        l (println "data: " data)
        filters (reduce 
                  (fn [re param]
                    (let [pkeyword (keyword (first param))
                          l (println "pkeyword: " pkeyword)
                          l (println "params " params)
                          pval (if (= (:type (last param)) "timestamp")
                                   (unparse (formatter "MM/dd/yy")
                                            (from-long (long (Float/parseFloat (pkeyword params)))))
                                   (pkeyword params))]
                       (str re (:label (last param)) ": " pval ". ")))
                  "" (:input-form data))]
        [(:info data) filters]))
            
(defn ids-to-keywords [ids]
  (map #(keyword (str (int %))) ids))






