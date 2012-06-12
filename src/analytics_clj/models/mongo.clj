(ns mongo
  (:use somnium.congomongo))

(defn connect
  ([db host port]
   (def conn (make-connection db :host host :port port))
   (set-connection! conn))
  ([db]
   (connect db "127.0.0.1" 27017)))

 
(defn get-cursor [collection options]
  (.iterator (apply fetch collection options)))

(defn has-next? [cursor]
  (.hasNext cursor))

(defn get-next [cursor]
  (somnium.congomongo.coerce/coerce (.next cursor) [:mongo :clojure]))





