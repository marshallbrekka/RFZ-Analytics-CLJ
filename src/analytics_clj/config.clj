(ns analytics-clj.config)
(def test-env {
    :mongo-offset ["mydb" "10.10.10.106" 27019]
    :mongo-balances ["mydb"]
    :data-files "/rfz/analytics"
              })
(def prod-env {
    :mongo-offset ["mydb"]
    :mongo-balances ["mydb"]
    :data-files "/home/rfz/debtapp/shared/ext/datalytics"
               })

(def conf (if (get (into {} (System/getenv)) "RFZ_ANALYTICS_DEV") test-env prod-env))
