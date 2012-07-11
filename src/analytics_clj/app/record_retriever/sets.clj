(ns analytics-clj.app.record-retriever.sets
  (:require [cheshire.core :as json]
            [clj-http.client :as client]
            [analytics-clj.app.file-io :as file-io]))
(def schema-url "https://beta.readyforzero.com/api/stat")
(def secret (file-io/read-first-line (file-io/open "secret.txt")))

(defn create-schema-keys [schema]
  (cond (= (type schema) (type {}))
          (apply merge (map (fn [[a b]] {(keyword a) (create-schema-keys b)}) schema))
        (= (type schema) (type []))
          (vec (map create-schema-keys schema))
        :else
          schema))

    

(def schema (update-in (create-schema-keys (json/parse-string (:body (client/get (str schema-url "/json?secret=" secret))))) [:endpoints] 
                                          (fn [eps] (filter (fn [ep] (not= "/json" (last (:route ep)))) eps))))
(defn get-routes []
  (conj (map (fn [ept] {:value (last (:route ept)) :label (:info ept) :options (:in ept)}) (:endpoints schema)) {:value "all-users" :label "Get All Users" :options []}))

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

(defn get-subset [params]
    (let [route (:set params)
          params (dissoc params :set)]
      (println route)
      (println params)

    (let [params (apply merge (map (fn [[k v]] {k (process-param route (name k) v)}) params))
          ids (:body 
                                   (client/post (str schema-url route) 
                                        {:headers {"Content-Type" "application/json" "Cookie" "disable-csrf=true;"} 
                                         :body (json/generate-string (assoc params :secret secret))}))]
    ;(println "request" (json/generate-string (assoc params :secret secret)))
    ;(println ids)
    (map (fn [id] (get id "id")) (json/parse-string ids)))))


    
(defn ids-to-keywords [ids]
  (println "num from ids to keywrods " (count ids))
  (if (nil? ids)
      '()
  (map (fn [a] (keyword (str (int a)))) ids)))

