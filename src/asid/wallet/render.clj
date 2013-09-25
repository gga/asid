(ns asid.wallet.render
  (:use midje.sweet)

  (:require [asid.wallet.links :as awl])

  (:import [asid.wallet Wallet]))

(defn to-json [wallet]
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

