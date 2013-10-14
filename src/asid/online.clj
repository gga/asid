(ns asid.online
  (:use [ring.adapter.jetty]
        [asid.strings :as as])
  (:require asid))

(defn start-asid []
  (run-jetty #'asid/app {:port 8888 :join? false}))

(defn -main []
  (run-jetty #'asid/app {:port (Integer/parseInt (as/getenv "PORT" "8888"))
                         :join? false}))
