(ns asid.online
  (:use [ring.adapter.jetty])
  (:require asid))

(defn start-asid []
  (run-jetty #'asid/app {:port 8888 :join? false}))
