(ns asid.test.asid-facts
  (:use midje.sweet
        asid)

  (:require [clojure.data.json :as json]
            [ring.mock.request :as mr]
            [compojure.core :as compojure]
            [ring.util.response :as rur])

  (:require [asid.graph :as ag]
            [asid.calling-card :as cc]
            [asid.calling-card-repository :as ccr]
            [asid.connection-request :as cr]
            [asid.connection-request-repository :as crr]
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
                                 (mr/header "Accept" "application/vnd.org.asidentity.wallet+json")))
          body (json/read-str (:body resp) :key-fn keyword)]
      (:status resp) => 200
      (-> body :bag) => empty?)
    (let [wallet (wr/save (w/new-wallet "seed") (:repo t-app))
          resp ((:web t-app) (-> (mr/request :get (w/uri wallet))
                                 (mr/header "Accept" "text/html, application/xml")))]
      (:status resp) => 200)
    (let [wallet (wr/save (w/new-wallet "seed") (:repo t-app))
          resp ((:web t-app) (-> (mr/request :get (w/uri wallet))
                                 (mr/header "Accept" "application/vnd.org.asidentity.introduction+json")))
          intro-doc (json/read-str (:body resp) :key-fn keyword)]
      (:status resp) => 200
      (-> intro-doc :identity) => (:identity wallet)
      (-> intro-doc :key :public) => (-> wallet :key :public)
      (-> intro-doc :signatures :identity) =not=> nil?
      (-> intro-doc :bag) => nil?)))

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
          pool (tpr/save (tp/new-trust-pool "pool" ["name" "dob"]))]
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
             (compojure/GET "/other-id" []
                            (json/write-str {:links {:letterplate "http://example.com/other-id/letterplate"}}))
             (compojure/POST "/other-id/letterplate" []
                             (-> (rur/response "Received calling card.")
                                 (rur/status 201)
                                 (rur/header "Location" "http://example.com/conn-req"))))
    (let [t-app (app)]
      (let [initiator (wr/save (w/new-wallet "initiator") (:repo t-app))
            pool (tpr/save (tp/new-trust-pool "pool" ["name"]))]
        (ag/trustpool pool initiator)
        (let [resp ((:web t-app) (-> (mr/request :post (tp/uri initiator pool)
                                                 (json/write-str {:uri "http://example.com/other-id"
                                                                  :identity "other-id"}))
                                     (mr/header "Content-Type" "application/vnd.org.asidentity.calling-card+json")))
              body (json/read-str (:body resp) :key-fn keyword)]
          (:status resp) => 201
          (-> resp :headers (get "Location")) => (-> body :links :self)
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

(fact "GET /<wallet-id>/card/<card-id>"
  (let [t-app (app)]
    (let [wallet (wr/save (w/new-wallet "seed") (:repo t-app))
          pool (tpr/save (tp/new-trust-pool "name" ["challenge"]))
          card (ccr/save (cc/new-calling-card "target-uri" "target-id"))]
      (ag/trustpool pool wallet)
      (ag/adds-identity card pool)
      (let [resp ((:web t-app) (-> (mr/request :get (cc/uri card wallet))
                                   (mr/header "Accept" "application/vnd.org.asidentity.calling-card+json")))
            body (json/read-str (:body resp) :key-fn keyword)]
        (:status resp) => 200
        (-> body :identity) =not=> nil?
        (-> body :otherParty) => "target-id"
        (-> body :links :self) => (cc/uri card wallet)
        (-> body :links :trustpool) => (tp/uri wallet pool)))))

(fact "GET /<wallet-id/request/<conn-req-id>"
  (let [t-app (app)]
    (let [wallet (wr/save (w/new-wallet "seed") (:repo t-app))
          conn-req (crr/save (cr/new-connection-request {:from "initiator-id"
                                                         :trust {:name "pool"
                                                                 :identity "pool-id"
                                                                 :challenge ["challenge"]}
                                                         :links {:initiator "initiator-uri"
                                                                 :self "calling-card-uri"}}))]
      (ag/requests-connection conn-req wallet)
      (let [resp ((:web t-app) (-> (mr/request :get (cr/uri conn-req wallet))
                                   (mr/header "Accept" "application/vnd.org.asidentity.connection-request+json")))
            body (json/read-str (:body resp) :key-fn keyword)]
        (:status resp) => 200
        (-> body :pool :name) => "pool"
        (-> body :pool :identity) => "pool-id"
        (-> body :pool :challenge) => ["challenge"]
        (-> body :links :self) => (cr/uri conn-req wallet)
        (-> body :links :from) => "initiator-uri"
        (-> body :links :callingCard) => "calling-card-uri"))))

(fact "PUT /<wallet-id>/request/<conn-req-id>"
  (let [t-app (app)]
    (hm/with-mock-http-server
      (hm/mock "http://example.com"
               (compojure/GET "/calling-card" []
                              (json/write-str {:links {:challenge "http://example.com/challenge"}}))
               (compojure/PUT "/challenge" []
                              (json/write-str {:signatures {:identity "signed-trustee-id"
                                                            :challenge "signed-value-value"}
                                               :bag {:challenge "initiator's value"}})))

      (let [wallet (wr/save (w/add-data (w/new-wallet "seed")
                                        "challenge" "sample") (:repo t-app))
            conn-req (crr/save (cr/new-connection-request {:from "initiator-id"
                                                           :trust {:name "pool"
                                                                   :identity "pool-id"
                                                                   :challenge ["challenge"]}
                                                           :links {:initiator "initiator-uri"
                                                                   :self "http://example.com/calling-card"}}))]
        (ag/requests-connection conn-req wallet)
        (let [resp ((:web t-app) (-> (mr/request :put (cr/uri conn-req wallet) (json/write-str {:accepted true}))
                                     (mr/header "Content-Type" "application/vnd.org.asidentity.connection-request+json")))]
          (:status resp) => 200)))))
