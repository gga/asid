(ns asid.render
  (:use midje.sweet)

  (:require [asid.trust-pool :as tp])

  (:import [asid.wallet Wallet]
           [asid.trust_pool TrustPool]))

(defprotocol Linked
  (links [this] "Returns a map of generated links for the object."))

(defprotocol Resource
  (to-json [this] "JSON representation of the resource")
  (content-type [this] "Content type string for the resource"))

(extend-type Wallet
  Resource

  (to-json [wallet]
    {:identity (:identity wallet)
     :bag (:bag wallet)
     :signatures (:signatures wallet)
     :key {:public (-> wallet :key :public)}})

  (content-type [_]
    "application/vnd.org.asidentity.wallet+json"))

(fact
  (let [tw (Wallet. "id" {} {} {:public "pub-key" :private "priv-key"})]
    (to-json tw) => (contains {:identity "id"})
    (to-json tw) => (contains {:key (contains {:public "pub-key"})})
    (:key (to-json tw)) =not=> (contains {:private "priv-key"})))

(extend-type TrustPool
  Resource

  (to-json [pool]
    {:name (:name pool)
     :identity (:identity pool)
     :challenge (:challenge pool)})

  (content-type [_]
    "application/vnd.org.asidentity.trust-pool+json"))

(extend-type TrustPool
  Linked

  (links [pool]
    (tp/links pool)))
