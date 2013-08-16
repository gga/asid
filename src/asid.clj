(ns asid
  (:use compojure.core)

  (:require [compojure.route :as route]
            [compojure.handler :as handler]))

(defroutes main-routes
  (GET "/favicon.ico" [] "")

  (route/not-found (File. "resources/public/not-found.html")))

(def app
  (-> (handler/site main-routes)
      (wrap-resource "public")
      (wrap-file-info)))


