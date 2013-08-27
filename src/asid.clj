(ns asid
  (:use compojure.core
        ring.middleware.resource
        ring.middleware.file-info)

  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [net.cgrand.enlive-html :as enlive])

  (:import java.io.File))

(enlive/deftemplate home "home.html" [])

(defroutes main-routes
  (GET "/favicon.ico" [] "")

  (GET "/" [] (apply str (home)))

  (route/not-found (File. "resources/public/not-found.html")))

(def app
  (-> (handler/site main-routes)
      (wrap-resource "public")
      (wrap-file-info)))


