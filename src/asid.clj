(ns asid
  (:use compojure.core
        ring.middleware.resource
        ring.middleware.file-info
        ring.util.response
        midje.sweet)

  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [net.cgrand.enlive-html :as enlive]
            [ring.mock.request :as mr])

  (:require [asid.wallet :as w]
            [asid.wallet-repository :as wr])

  (:import java.io.File))

(enlive/deftemplate home "home.html" [])

(def wallet-repo (wr/initialize!))

(defroutes main-routes
  (GET "/favicon.ico" [] "")

  (GET "/" [] (apply str (home)))

  (POST "/identity" []
        (let [new-id (wr/save wallet-repo (w/new-wallet))]
          (assoc (created (w/uri new-id))
            :body new-id)))

  (route/not-found (File. "resources/public/not-found.html")))

(def app
  (-> (handler/site main-routes)
      (wrap-resource "public")
      (wrap-file-info)))

(fact "/"
  (:status (app (mr/request :get "/"))) => 200)

(fact "/identity"
  (:status (app (mr/request :post "/identity"))) => 201
  (get (:headers (app (mr/request :post "/identity"))) "Location") => #"\/[a-z0-9-]+$")


