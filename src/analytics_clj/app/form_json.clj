(ns analytics-clj.app.form-json
  (:require [cheshire.core :as json]))

(declare process-names-helper-map)
(declare process-names-helper-col)
(declare process-names)

(defn build-json [& objects]
  (json/generate-string (map (fn [v] 
     (process-names (first v) (last v))) objects)))


(defn process-names [name-space items]
  (process-names-helper-col (str "[" name-space "]") items))

(defn process-names-helper-map [pre k v]
  {k (if (= k :name)
    (str pre "[" v "]")
    (process-names-helper-col pre v))})

(defn process-names-helper-col [pre v]
  (if (coll? v)
     (if (map? v)
      (apply merge (map (fn [[k v]] (process-names-helper-map pre k v)) v))
      (map #(process-names-helper-col pre %) v))
    v))


