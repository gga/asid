(ns asid
  (:use compojure.core
        ring.middleware.resource
        ring.middleware.file-info
        ring.util.response
        ring.adapter.jetty
        midje.sweet)

  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.data.json :as json]
            [clojure.string :as cs]
            [ring.mock.request :as mr])

  (:require [asid.wallet :as w]
            [asid.wallet-repository :as wr])

  (:import java.io.File))

(def wallet-repo (wr/initialize!))

(defroutes main-routes
  (GET "/favicon.ico" [] "")

  (POST "/identity" [_ :as {body :body}]
        (let [id-seed (slurp body)]
          (if (= 0 (count id-seed))
            (-> (response "Identity seed not supplied")
                (status 400))
            (let [new-id (wr/save wallet-repo (w/new-wallet id-seed))]
              (created (w/uri new-id))))))

  (GET ["/:id", :id w/wallet-identity-grammar] [id :as {accepts :accepts}]
       (let [wallet-json (json/write-str (w/to-json (wr/get-wallet wallet-repo id)))
             content-handlers {"text/html" (fn [_] (File. "resources/public/wallet/index.html"))
              
                               "application/vnd.org.asidentity.wallet+json"
                               (fn [content] (-> (response content)
                                                 (content-type "application/vnd.org.asidentity.wallet+json")))}

             handler (first (remove nil? (map content-handlers accepts)))]
         (handler wallet-json)))

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

(defn- wrap-accept [handler]
  (fn [req]
    (handler (assoc req :accepts
                    (map cs/trim (cs/split (-> req :headers (get "accept" "*/*"))
                                           #","))))))

(fact
  (let [wrapper (wrap-accept #(:accepts %))
        req (mr/request :get "body")]
    (wrapper (mr/header req "Accept" "text/html")) => ["text/html"]
    (wrapper (mr/header req "Accept" "text/html, */*")) => ["text/html" "*/*"]))

(defn- wrap-vary [handler]
  #(header (handler %) "Vary" "Accept"))

(def app
  (-> (handler/site main-routes)
      wrap-accept
      wrap-vary
      (wrap-resource "public")
      wrap-file-info
      wrap-dir-index))

(fact "/"
  (:status (app (mr/request :get "/"))) => 200)

(fact "/identity"
  (let [req (app (-> (mr/request :post "/identity")
                     (mr/body "sample-id-seed")))]
    (:status req) => 201
    (get (:headers req) "Location") => #"\/[a-z0-9-]+$")
  (let [req (app (-> (mr/request :post "/identity")
                     (mr/body "")))]
    (:status req) => 400))

(fact "/<wallet-id>"
  (let [wallet (wr/save wallet-repo (w/new-wallet "seed"))
        req (app (-> (mr/request :get (w/uri wallet))
                     (mr/header "Accept" "text/html, application/xml")))]
    (:status req) => 200))

(defn start-asid []
  (run-jetty #'asid/app {:port 8888 :join? false}))
