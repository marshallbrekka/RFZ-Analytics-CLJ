(ns mongo
  (:use 
    [somnium.congomongo :as cm :only [make-connection set-connection! fetch]]
    [somnium.congomongo.coerce :as cmc :only [coerce]])
  (:require 
    ;[app.config :as config]
    ;[app.util.io :as io]
    [clojure.data.csv :as csv]
    [clojure.tools.logging :as lg])
  )

(defn connect
  ([db host port]
   (def conn (make-connection db :host host :port port))
   (set-connection! conn))
  ([db]
   (connect db "127.0.0.1" 27017)))

 
(defn get-cursor [collection options]
  ;(assoc options :as :mongo)
  (apply fetch collection (mapcat identity options)))


(defn has-next? [cursor]
  (.hasNext cursor))

(defn get-next [cursor]
  (somnium.congomongo.coerce/coerce (.next cursor) [:mongo :clojure]))





