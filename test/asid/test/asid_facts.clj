(ns asid.test.asid-facts
  (:use midje.sweet
        asid)

  (:require [clojure.data.json :as json]
            [ring.mock.request :as mr])

  (:require [asid.nodes :as an]
            [asid.trust-pool :as tp]
            [asid.trust-pool-repository :as tpr]
            [asid.wallet :as w]
            [asid.wallet.repository :as wr]))

(fact "GET /"
  (:status (app (mr/request :get "/"))) => 200)

(fact "POST /identity"
  (let [req (app (-> (mr/request :post "/identity")
                     (mr/body "sample-id-seed")))]
    (:status req) => 201
    (-> req :headers (get "Location")) => #"\/[a-z0-9-]+$")
  (let [req (app (-> (mr/request :post "/identity")
                     (mr/body "")))]
    (:status req) => 400))

(fact "GET /<wallet-id>"
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

(fact "POST /<wallet-id>/bag"
  (let [wallet (wr/save (w/new-wallet "seed") repo)
        req (app (mr/request :post (w/bag-uri wallet) {"key" "key1" "value" "new-value"}))]
    (:status req) => 200))

(fact "POST /<wallet-id>/trustpool"
  (let [wallet (wr/save (w/new-wallet "seed") repo)
        req (app (-> (mr/request :post (w/trustpool-uri wallet)
                                 (json/write-str {:name "hello" :challenge ["name"]}))
                     (mr/header "Content-Type" "application/vnd.org.asidentity.trust-pool+json")))]
    (:status req) => 201))

(fact "GET /<wallet-id>/trustpool/<pool-id>"
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


