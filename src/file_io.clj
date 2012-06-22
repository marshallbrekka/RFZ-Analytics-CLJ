(ns file-io 
  (:require [cheshire.core :as json])
  (:import (java.io BufferedReader FileReader BufferedWriter FileWriter)))

(defn read-lines [file]
  (apply merge (pmap (fn [a] (json/parse-string a true)) (line-seq file))))


(defn write-lines [file data]
    (println "in writer " (count data))
    (doseq [[k v] data] (.write file (str (json/generate-string {k v}) "\n"))))

(defn write-line [file line]
  (.write file (str (json/generate-string line) "\n")))


(defn open [filename]
  (BufferedReader. (FileReader. filename)))

(defn open-write [filename]
  (BufferedWriter. (FileWriter. filename)))

(defn close [file]
  (println "closing file")
  (.close file))
