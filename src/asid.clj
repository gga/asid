(ns asid
  (:use compojure.core
        ring.middleware.resource
        ring.middleware.file-info
        ring.util.response
        midje.sweet)

  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.data.json :as json]
            [ring.mock.request :as mr])

  (:require [asid.wallet :as w]
            [asid.wallet-repository :as wr])

  (:import java.io.File))

(def wallet-repo (wr/initialize!))

(defroutes main-routes
  (GET "/favicon.ico" [] "")

  (POST "/identity" []
        (let [new-id (wr/save wallet-repo (w/new-wallet))]
          (created (w/uri new-id))))

  (GET ["/:id", :id #"([0-9a-f]+-){4,4}[0-9a-f]+"] [id]
       (-> (response (json/write-str (w/to-json (wr/get-wallet wallet-repo id))))
           (content-type "application/org.asidentity.wallet+json")))

  (route/not-found (File. "resources/public/not-found.html")))

(defn wrap-dir-index [handler]
  (fn [req]
    (handler
     (update-in req [:uri] #(if (re-find #"/$" %)
                              (str % "index.html")
                              %)))))

(fact
  (let [wrapper (wrap-dir-index #(:uri %))]
    (wrapper {:uri "/"}) => "/index.html"
    (wrapper {:uri "/path/"}) => "/path/index.html"
    (wrapper {:uri "/path/sub/"}) => "/path/sub/index.html"
    (wrapper {:uri "/path"}) => "/path"))

(def app
  (-> (handler/site main-routes)
      (wrap-resource "public")
      wrap-file-info
      wrap-dir-index))

(fact "/"
  (:status (app (mr/request :get "/"))) => 200)

(fact "/identity"
  (let [req (app (mr/request :post "/identity"))]
    (:status req) => 201
    (get (:headers req) "Location") => #"\/[a-z0-9-]+$"))


