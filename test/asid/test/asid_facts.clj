(ns asid.test.asid-facts
  (:use midje.sweet
        asid)

  (:require [clojure.data.json :as json]
            [ring.mock.request :as mr]
            [compojure.core :as cc]
            [ring.util.response :as rur])

  (:require [asid.graph :as ag]
            [asid.trust-pool :as tp]
            [asid.trust-pool-repository :as tpr]
            [asid.wallet :as w]
            [asid.wallet.repository :as wr]
            [asid.test.http-mock :as hm]))

(fact "GET /"
  (:status ((:web (app)) (mr/request :get "/"))) => 200)

(fact "POST /identity"
  (let [resp ((:web (app)) (-> (mr/request :post "/identity")
                               (mr/body "sample-id-seed")))]
    (:status resp) => 201
    (-> resp :headers (get "Location")) => #"\/[a-z0-9-]+$")
  (let [resp ((:web (app)) (-> (mr/request :post "/identity")
                               (mr/body "")))]
    (:status resp) => 400))

(fact "GET /<wallet-id>"
  (let [t-app (app)]
    (let [resp ((:web t-app) (-> (mr/request :get "/bada-bada-bada-1d")
                                 (mr/header "Accept" "application/vnd.org.asidentity.wallet+json")))]
      (:status resp) => 404
      (:body resp) => "Not found.")
    (let [wallet (wr/save (w/new-wallet "seed") (:repo t-app))
          resp ((:web t-app) (-> (mr/request :get (w/uri wallet))
                                 (mr/header "Accept" "text/html, application/xml")))]
      (:status resp) => 200)
    (let [wallet (wr/save (w/new-wallet "seed") (:repo t-app))
          resp ((:web t-app) (-> (mr/request :get (w/uri wallet))
                                 (mr/header "Accept" "application/vnd.org.asidentity.introduction+json")))
          intro-doc (json/read-str (:body resp))]
      (:status resp) => 200
      (-> intro-doc (get "bag")) => nil?)))

(fact "POST /<wallet-id>/bag"
  (let [t-app (app)]
    (let [wallet (wr/save (w/new-wallet "seed") (:repo t-app))
          resp ((:web t-app) (mr/request :post (w/bag-uri wallet) {"key" "key1" "value" "new-value"}))]
      (:status resp) => 200)))

(fact "POST /<wallet-id>/trustpool"
  (let [t-app (app)]
    (let [wallet (wr/save (w/new-wallet "seed") (:repo t-app))
          resp ((:web t-app) (-> (mr/request :post (w/trustpool-uri wallet)
                                             (json/write-str {:name "hello" :challenge ["name"]}))
                                 (mr/header "Content-Type" "application/vnd.org.asidentity.trust-pool+json")))
          pool-doc (json/read-str (:body resp) :key-fn keyword)]
      (:status resp) => 201
      (:name pool-doc) => "hello"
      (:challenge pool-doc) => ["name"])))

(fact "GET /<wallet-id>/trustpool/<pool-id>"
  (let [t-app (app)]
    (let [wallet (wr/save (w/new-wallet "seed") (:repo t-app))
          pool (tpr/save (tp/new-trust-pool "pool" ["name" "dob"]) (:repo t-app))]
      (ag/trustpool pool wallet)
      (let [resp ((:web t-app) (-> (mr/request :get (tp/uri wallet pool))
                                   (mr/header "Accept" "application/vnd.org.asidentity.trust-pool+json")))]
        (:status resp) => 200)
      (let [resp ((:web t-app) (-> (mr/request :get (str (w/uri wallet) "/trustpool/bada-bada-bada-1d"))
                                   (mr/header "Accept" "application/vnd.org.asidentity.trust-pool+json")))]
        (:status resp) => 404
        (:body resp) => "Not found."))))

(fact "POST /<wallet-id>/trustpool/<pool-id>"
  (hm/with-mock-http-server
    (hm/mock "http://example.com"
             (cc/GET "/other-id" []
                     (json/write-str {:links {:letterplate "http://example.com/other-id/letterplate"}}))
             (cc/POST "/other-id/letterplate" []
                      (-> (rur/response "Received calling card.")
                          (rur/status 201)
                          (rur/header "Location" "http://example.com/conn-req"))))
    (let [t-app (app)]
      (let [initiator (wr/save (w/new-wallet "initiator") (:repo t-app))
            pool (tpr/save (tp/new-trust-pool "pool" ["name"]) (:repo t-app))]
        (ag/trustpool pool initiator)
        (let [resp ((:web t-app) (-> (mr/request :post (tp/uri initiator pool)
                                                 (json/write-str {:uri "http://example.com/other-id"
                                                                  :identity "other-id"}))
                                     (mr/header "Content-Type" "application/vnd.org.asidentity.calling-card+json")))
              body (json/read-str (:body resp) :key-fn keyword)]
          (:status resp) => 201
          (-> body :otherParty) => "other-id"
          (-> body :links :self) =contains=> (-> body :identity))))))

(fact "POST /<wallet-id>/letterplate"
  (let [t-app (app)]
    (let [conn-req {:from "initiator-id"
                    :trust {:name "pool"
                            :identity "pool-id"
                            :challenge ["key"]}
                    :links {:self "calling-card-uri"
                            :initiator "initiator-wallet-uri"}}
          trustee (wr/save (w/new-wallet "trustee") (:repo t-app))
          resp ((:web t-app) (-> (mr/request :post (w/letterplate-uri trustee)
                                             (json/write-str conn-req))
                                 (mr/header "Content-Type" "vnd/application.org.asidentity.connection-request+json")))]
      (:status resp) => 201
      (-> resp :headers (get "Location")) => (re-pattern (str (w/uri trustee) "/request/[a-f0-9-]+$")))))
