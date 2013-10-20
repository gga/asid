(ns asid.wallet.links
  (:use midje.sweet)

  (:require [asid.nodes :as an]
            [asid.trust-pool :as tp]
            [asid.wallet :as w]
            [asid.render :as render])

  (:import [asid.wallet Wallet]))

(defn add-link [so-far key func wallet]
  (conj so-far [key (func wallet)]))

(defn self-link [so-far wallet]
  (add-link so-far :self w/uri wallet))

(defn bag-link [so-far wallet]
  (add-link so-far :bag w/bag-uri wallet))

(defn trustpool-link [so-far wallet]
  (add-link so-far :trustpool w/trustpool-uri wallet))

(defn all-trustpool-links [so-far wallet]
  (conj so-far [:trustpools (map #(tp/uri wallet %)
                                 (an/sub-objects wallet :trustpool))]))

(defn letterplate-link [so-far wallet]
  (add-link so-far :letterplate w/letterplate-uri wallet))

(extend-type Wallet
  render/Linked

  (links [wallet]
    (-> {}
        (self-link wallet)
        (bag-link wallet)
        (trustpool-link wallet)
        (all-trustpool-links wallet)
        (letterplate-link wallet))))

(fact
  (let [tw (Wallet. "id" {} {} {:public "pub-key" :private "priv-key"})]
    (render/links tw) => (contains {:bag "/id/bag"})
    (render/links tw) => (contains {:trustpool "/id/trustpool"}))

  (let [wallet (w/new-wallet "seed")]
    (:self (render/links wallet)) => (str "/" (:identity wallet))))
