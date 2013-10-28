(ns asid.online
  (:use [ring.adapter.jetty]
        [asid.strings :as as])
  (:require asid))

(defn start-asid
  ([] (start-asid 8888))
  ([port] (run-jetty (:web (asid/app))
                     {:port port :join? false})))

(defn -main []
  (start-asid (Integer/parseInt (as/getenv "PORT" "8888"))))
