(ns asid.render
  (:use midje.sweet)

  (:require [asid.wallet.links :as awl]
            [asid.trust-pool :as tp]
            [asid.calling-card :as cc])

  (:import [asid.wallet Wallet]
           [asid.trust_pool TrustPool]
           [asid.calling_card CallingCard]))

(defmulti to-json type)

(defmethod to-json :default [coll]
  coll)

(fact
  (to-json {:k "v"}) => {:k "v"})

(defmethod to-json Wallet [wallet]
  {:identity (:identity wallet)
   :bag (:bag wallet)
   :signatures (:signatures wallet)
   :key {:public (-> wallet :key :public)}
   :links (awl/wallet-links wallet)})

(fact
  (let [tw (Wallet. "id" {} {} {:public "pub-key" :private "priv-key"})]
    (to-json tw) => (contains {:identity "id"})
    (to-json tw) => (contains {:key (contains {:public "pub-key"})})
    (:key (to-json tw)) =not=> (contains {:private "priv-key"})
    (:links (to-json tw)) => (contains {:bag "/id/bag"})
    (:links (to-json tw)) => (contains {:trustpool "/id/trustpool"})))

(defmethod to-json TrustPool [pool]
  {:name (:name pool)
   :identity (:identity pool)
   :challenge (:challenge pool)
   :links (tp/links pool)})

(defmethod to-json CallingCard [card]
  {:identity (:identity card)
   :otherParty (:other-party card)
   :links (cc/links card)})
