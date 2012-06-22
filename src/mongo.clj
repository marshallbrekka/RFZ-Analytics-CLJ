(ns mongo
  (:use 
    [somnium.congomongo :as cm :only [make-connection set-connection! fetch distinct-values mass-insert!]]
    [somnium.congomongo.coerce :as cmc :only [coerce]])
  (:require 
    ;[app.config :as config]
    ;[app.util.io :as io]
    [clojure.data.csv :as csv]
    [clojure.tools.logging :as lg])
  )

(defn connect
  ([db host port]
   (make-connection db :host host :port port))
  ([db]
   (connect db "127.0.0.1" 27017)))

 
(defn run-query [conn collection options]
  (cm/with-mongo conn
      (apply fetch collection (mapcat identity options))))

(defn get-distinct [conn collection dis-key]
  (cm/with-mongo conn
      (distinct-values collection dis-key)))


(defn has-next? [cursor]
  (.hasNext cursor))

(defn get-next [cursor]
  (cmc/coerce (.next cursor) [:mongo :clojure]))

(defn insert [conn collection batch]
  (cm/with-mongo conn
      (mass-insert! collection batch)))




