(ns asid
  (:use compojure.core
        ring.middleware.resource
        ring.middleware.file-info
        ring.util.response
        midje.sweet)

  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.data.json :as json]
            [clojure.string :as cs]
            [ring.mock.request :as mr])

  (:require [asid.neo :as an]
            [asid.wallet :as w]
            [asid.identity :as aid]
            [asid.wallet-repository :as wr]
            [asid.content-negotiation :as acn]
            [asid.static :as as]
            [asid.trust-pool :as tp]
            [asid.trust-pool-repository :as tpr])

  (:import java.io.File))

(def repo (an/initialize!))

(defroutes main-routes
  (GET "/favicon.ico" [] "")

  (POST "/identity" [_ :as {body :body}]
        (let [id-seed (slurp body)]
          (if (= 0 (count id-seed))
            (-> (response "Identity seed not supplied")
                (status 400))
            (let [new-id (wr/save repo (w/new-wallet id-seed))]
              (created (w/uri new-id))))))

  (GET ["/:id", :id aid/grammar] [id :as {accepts :accepts}]
       (let [wallet-json (json/write-str (w/to-json (wr/get-wallet repo id)))
             content-handlers {"text/html" (fn [_] (File. "resources/public/wallet/index.html"))
              
                               "application/vnd.org.asidentity.wallet+json"
                               (fn [content] (-> (response content)
                                                 (content-type "application/vnd.org.asidentity.wallet+json")))}

             handler (first (remove nil? (map content-handlers accepts)))]
         (handler wallet-json)))

  (POST ["/:id/bag", :id aid/grammar] [id key value]
        (let [wallet (wr/get-wallet repo id)]
          (if (or (= 0 (count key))
                  (= 0 (count value)))
            (-> (response "Either key or value or both not supplied.")
                (status 400))
            (-> (response (json/write-str (w/to-json (wr/save repo (w/add-data wallet key value)))))
                (content-type "application/vnd.org.asidentity.wallet+json")))))

  (POST ["/:id/trustpool", :id aid/grammar] [id :as {body :body}]
        (let [wallet (wr/get-wallet repo id)
              pool-doc (json/read-str (slurp body))
              name (get pool-doc "name")]
          (if (= 0 (count name))
            (-> (response "A name must be provided.")
                (status 400))
            (let [challenge-keys (get pool-doc "challenge")
                  pool (tpr/save repo (tp/new-trust-pool name challenge-keys))]
              (an/connect-nodes wallet pool :trustpool)
              (-> (response (json/write-str (tp/to-json pool)))
                  (content-type "application/vnd.org.asidentity.trust-pool+json")
                  (header "Location" (tp/uri pool))
                  (status 201))))))

  (route/not-found (File. "resources/public/not-found.html")))

(def app
  (-> (handler/site main-routes)
      acn/accepts
      acn/vary-by-accept
      (wrap-resource "public")
      wrap-file-info
      as/static-dir-index))

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
  (let [wallet (wr/save repo (w/new-wallet "seed"))
        req (app (-> (mr/request :get (w/uri wallet))
                     (mr/header "Accept" "text/html, application/xml")))]
    (:status req) => 200))

(fact "/<wallet-id>/bag"
  (let [wallet (wr/save repo (w/new-wallet "seed"))
        req (app (mr/request :post (w/bag-uri wallet) {"key" "key1" "value" "new-value"}))]
    (:status req) => 200))

(fact "/<wallet-id>/trustpool"
  (let [wallet (wr/save repo (w/new-wallet "seed"))
        req (app (-> (mr/request :post (w/trustpool-uri wallet)
                                 (json/write-str {:name "hello" :challenge ["name"]}))))]
    (:status req) => 201))
