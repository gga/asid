(ns asid.test.asid-facts
  (:use midje.sweet
        asid)

  (:require [clojure.data.json :as json]
            [ring.mock.request :as mr])

  (:require [asid.graph :as ag]
            [asid.trust-pool :as tp]
            [asid.trust-pool-repository :as tpr]
            [asid.wallet :as w]
            [asid.wallet.repository :as wr]))

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
                                 (mr/header "Content-Type" "application/vnd.org.asidentity.trust-pool+json")))]
      (:status resp) => 201)))

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
  (let [t-app (app)]
    (let [initiator (wr/save (w/new-wallet "initiator") (:repo t-app))
          pool (tpr/save (tp/new-trust-pool "pool" ["name"]) (:repo t-app))
          trustee (wr/save (w/new-wallet "trustee") (:repo t-app))]
      (ag/trustpool pool initiator)
      (let [resp ((:web t-app) (-> (mr/request :post (tp/uri initiator pool)
                                               (json/write-str {:uri (w/uri trustee)
                                                                :identity (:identity trustee)}))
                                   (mr/header "Content-Type" "application/vnd.org.asidentity.calling-card+json")))]
        (:status resp)) => 201)))
