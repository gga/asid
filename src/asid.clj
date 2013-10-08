(ns asid
  (:use compojure.core
        ring.middleware.resource
        ring.middleware.file-info
        ring.util.response
        midje.sweet
        [asid.error.thread :only [fail-> dofailure]]
        [asid.error.definition :only [validate!]])

  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.data.json :as json]
            [clojure.string :as cs]
            [ring.mock.request :as mr])

  (:require [asid.neo :as an]
            [asid.identity :as aid]
            [asid.trust-pool :as tp]
            [asid.wallet :as w]
            [asid.calling-card :as cc]
            [asid.introduction :as i]
            [asid.connection-request :as conn]
            [asid.wallet.links :as awl]
            [asid.wallet.repository :as wr]
            [asid.content-negotiation :as acn]
            [asid.json-doc-exchange :as jde]
            [asid.static :as as]
            [asid.current-request :as req]
            [asid.response :as ar]
            [asid.trust-pool-repository :as tpr]
            [asid.calling-card-repository :as ccr]
            [asid.connection-request-repository :as cr])

  (:import java.io.File))

(def repo (an/initialize!))

(defroutes main-routes
  (POST "/identity" [_ :as {body :body}]
        (let [id-seed (slurp body)]
          (fail-> {"id seed" id-seed}
                  (validate! :not-empty "id seed")
                  (get "id seed")
                  w/new-wallet
                  (wr/save repo)
                  ar/created)))

  (GET ["/:id", :id aid/grammar] [id :as {accepts :accepts}]
       (let [content {"text/html"
                      (fn [] (File. "resources/public/wallet/index.html"))

                      "application/vnd.org.asidentity.wallet+json"
                      (fn [] (fail->
                              (wr/get-wallet id repo)
                              ar/resource))

                      "application/vnd.org.asidentity.introduction+json"
                      (fn [] (fail-> (wr/get-wallet id repo)
                                     (i/intro-to-wallet)
                                     ar/resource))}

             handler (first (remove nil? (map content accepts)))]
         (handler)))

  (POST ["/:id/bag", :id aid/grammar] [id key value]
        (fail-> {"id" id, "key" key, "value" value}
                (validate! :not-empty "key")
                (validate! :not-empty "value")
                (get "id")
                (wr/get-wallet repo)
                (w/add-data key value)
                (wr/save repo)
                ar/resource))

  (POST ["/:id/trustpool", :id aid/grammar] [id :as {pool-doc :json-doc}]
        (dofailure
         [data (validate! pool-doc :not-empty "name")
          name (get data "name")
          challenge-keys (get data "challenge")
          pool (tpr/save (tp/new-trust-pool name challenge-keys) repo)
          wallet (wr/get-wallet id repo)
          conn (an/connect-nodes wallet pool :trustpool)]
         (ar/created pool)))

  (GET "/:walletid/trustpool/:poolid" [walletid poolid]
       (fail-> (wr/get-wallet walletid repo)
               (tpr/pool-from-wallet poolid)
               ar/resource))

  (POST "/:walletid/trustpool/:poolid" [walletid poolid :as {calling-card :json-doc}]
        (dofailure [wallet (wr/get-wallet walletid repo)
                    pool (tpr/pool-from-wallet wallet poolid)]
                   (fail-> (cc/new-calling-card (:identity calling-card)
                                                (:uri calling-card))
                           (cc/submit wallet pool)
                           ccr/save
                           (cc/attach pool)
                           (ar/created "application/vnd.org.asidentity.calling-card+json"))))

  (POST ["/:id/letterplate", :id aid/grammar] [walletid :as {conn-req :json-doc}]
        (dofailure [wallet (wr/get-wallet walletid repo)] 
                   (-> (conn/new-connection-request conn-req)
                       cr/save
                       conn/attach wallet)))

  (route/not-found (File. "resources/public/not-found.html")))

(def app
  (-> (handler/site main-routes)
      req/capture-request
      jde/json-documents
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
    (-> req :headers (get "Location")) => #"\/[a-z0-9-]+$")
  (let [req (app (-> (mr/request :post "/identity")
                     (mr/body "")))]
    (:status req) => 400))

(fact "/<wallet-id>"
  (let [req (app (-> (mr/request :get "/bada-bada-bada-1d")
                     (mr/header "Accept" "application/vnd.org.asidentity.wallet+json")))]
    (:status req) => 404
    (:body req) => "Not found.")
  (let [wallet (wr/save (w/new-wallet "seed") repo)
        req (app (-> (mr/request :get (w/uri wallet))
                     (mr/header "Accept" "text/html, application/xml")))]
    (:status req) => 200)
  (let [wallet (wr/save (w/new-wallet "seed") repo)
        req (app (-> (mr/request :get (w/uri wallet))
                     (mr/header "Accept" "application/vnd.org.asidentity.introduction+json")))
        intro-doc (json/read-str (:body req))]
    (:status req) => 200
    (-> intro-doc (get "bag")) => nil?))

(fact "/<wallet-id>/bag"
  (let [wallet (wr/save (w/new-wallet "seed") repo)
        req (app (mr/request :post (w/bag-uri wallet) {"key" "key1" "value" "new-value"}))]
    (:status req) => 200))

(fact "/<wallet-id>/trustpool"
  (let [wallet (wr/save (w/new-wallet "seed") repo)
        req (app (-> (mr/request :post (w/trustpool-uri wallet)
                                 (json/write-str {:name "hello" :challenge ["name"]}))
                     (mr/header "Content-Type" "application/vnd.org.asidentity.trust-pool+json")))]
    (:status req) => 201))

(fact "/<wallet-id>/trustpool/<pool-id>"
  (let [wallet (wr/save (w/new-wallet "seed") repo)
        pool (tpr/save (tp/new-trust-pool "pool" ["name" "dob"]) repo)]
    (an/connect-nodes wallet pool :trustpool)
    (let [req (app (-> (mr/request :get (tp/uri wallet pool))
                       (mr/header "Accept" "application/vnd.org.asidentity.trust-pool+json")))]
      (:status req) => 200)
    (let [req (app (-> (mr/request :get (str (w/uri wallet) "/trustpool/bada-bada-bada-1d"))
                       (mr/header "Accept" "application/vnd.org.asidentity.trust-pool+json")))]
      (:status req) => 404
      (:body req) => "Not found.")))
