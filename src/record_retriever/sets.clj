(ns record-retriever.sets
  (:require [mongo]
            [cheshire.core :as json]
            [clj-http.client :as client]))
(def sets-collection "usersets")
(def conn (mongo/connect "analytics"))
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
(defn get-routes []
  (map (fn [ept] {:value (last (:route ept)) :label (:info ept) :options (:in ept)}) (:endpoints schema)))


(defn get-subset [route params]
  (println route params)
  (let [params (apply merge (map (fn [[k v]] {k (long (Float/parseFloat v))}) params))
        ids (json/parse-string (:body (client/post (str schema-url route) {:headers {"Content-Type" "application/json" "Cookie" "disable-csrf=true;"} :body (json/generate-string params)})))]
    (map (fn [id] (get id "id")) ids)))
    
(defn ids-to-keywords [ids]
  (map (fn [a] (keyword (str (int a)))) ids))

