(ns asid.render
  (:use midje.sweet)

  (:require [asid.wallet.links :as awl]
            [asid.trust-pool :as tp])

  (:import [asid.wallet Wallet]
           [asid.trust_pool TrustPool]))

(defprotocol Resource
  (to-json [this] "JSON representation of the resource")
  (content-type [this] "Content type string for the resource"))

(extend-type clojure.lang.PersistentArrayMap
  Resource
  (to-json [m]
    m)

  (content-type [m]
    "application/vnd.clojure.map+json"))

(fact
  (to-json {:k "v"}) => {:k "v"})

(extend-type Wallet
  Resource

  (to-json [wallet]
    {:identity (:identity wallet)
     :bag (:bag wallet)
     :signatures (:signatures wallet)
     :key {:public (-> wallet :key :public)}
     :links (awl/wallet-links wallet)})

  (content-type [_]
    "application/vnd.org.asidentity.wallet+json"))

(fact
  (let [tw (Wallet. "id" {} {} {:public "pub-key" :private "priv-key"})]
    (to-json tw) => (contains {:identity "id"})
    (to-json tw) => (contains {:key (contains {:public "pub-key"})})
    (:key (to-json tw)) =not=> (contains {:private "priv-key"})
    (:links (to-json tw)) => (contains {:bag "/id/bag"})
    (:links (to-json tw)) => (contains {:trustpool "/id/trustpool"})))

(extend-type TrustPool
  Resource

  (to-json [pool]
    {:name (:name pool)
     :identity (:identity pool)
     :challenge (:challenge pool)
     :links (tp/links pool)})

  (content-type [_]
    "application/vnd.org.asidentity.trust-pool+json"))
