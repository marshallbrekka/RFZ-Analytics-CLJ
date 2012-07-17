(defproject analytics-clj "0.1.0-SNAPSHOT"
            :description "FIXME: write this!"
            :dependencies [
                           [congomongo "0.1.9"]
                           [org.clojure/clojure "1.4.0"]
                           [com.novemberain/monger "1.0.0-rc1"]
                           [noir "1.3.0-beta7"]
                           [org.clojure/tools.logging "0.2.3"]
                           [clj-ical "1.1"]
                           [stencil "0.2.0"]
                           [congomongo "0.1.9"]
                           [korma "0.3.0-beta10"]
                           [clj-time "0.4.2"]
                           [clj-http "0.4.1"]
                           [postgresql "9.1-901-1.jdbc4"]
                           [ibdknox/clj-record "1.0.4"]
                           [incanter/incanter-core "1.3.0"]
                           [incanter/incanter-charts "1.3.0"]
                           [twitter-api "0.6.10"]
                           [trammel "0.8.0-SNAPSHOT"]
                           [expectations "1.4.3"]
                           [org.clojure/data.json "0.1.3"]
                           [com.notnoop.apns/apns "0.1.6"]
                           [org.jsoup/jsoup "1.6.2"]
                           [org.clojure/data.zip "0.1.1"]
                           [org.slf4j/slf4j-log4j12 "1.6.4"]
                           [org.clojure/data.csv "0.1.2"]
                           [log4j/apache-log4j-extras "1.1"]
                           [clj-serializer "0.1.3"]
                           [deep-freeze "1.2.2-SNAPSHOT"]
                           [cheshire "4.0.0"]
                           [clj-time "0.4.3"]
                                                     
                           ]
            :plugins [[lein-ring "0.7.1"]]
            :ring {:handler analytics-clj.server/handler}
            :main analytics-clj.server
            :warn-on-reflection true)

