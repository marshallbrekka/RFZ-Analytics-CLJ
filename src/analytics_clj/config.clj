(ns analytics-clj.config)
(def test-env {
    :mongo-offset ["mydb" "10.10.10.106" 27019]
    :mongo-balances ["mydb" "10.10.10.106" 27019] ;["mydb"]
    :data-files "/rfz/analytics"
    :endpoint-url "http://localhost:8001/api/stat"
              })
(def prod-env {
    :mongo-offset ["mydb" "ec2-23-22-195-8.compute-1.amazonaws.com" 27017]
    :mongo-balances ["mydb" "ec2-23-22-195-8.compute-1.amazonaws.com" 27017]
    :data-files "/home/rfz/debtapp/shared/ext/datalytics"
    :endpoint-url "https://beta.readyforzero.com/api/stat" 
               })

(def conf (if (get (into {} (System/getenv)) "RFZ_ANALYTICS_DEV") test-env prod-env))

