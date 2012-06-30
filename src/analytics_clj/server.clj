(ns analytics-clj.server
  (:require [noir.server :as server]
            [analytics-clj.views.welcome]))

(server/load-views-ns 'analytics-clj.views)

(defn -main [& m]
  (let [mode (keyword (or (first m) :dev))
        port (Integer. (get (System/getenv) "PORT" "8888"))]
    (server/start port {:mode mode
                        :ns 'analytics-clj})))

(def handler (server/gen-handler {:mode :dev
                  :ns 'analytics-clj}))

