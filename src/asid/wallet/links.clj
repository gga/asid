(ns asid.wallet.links
  (:use midje.sweet)

  (:require [asid.neo :as an]
            [asid.trust-pool :as tp]
            [asid.wallet :as w]))

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

(defn links []
  (fn [wallet]
    (-> {}
        (self-link wallet)
        (bag-link wallet)
        (trustpool-link wallet)
        (all-trustpool-links wallet)
        (letterplate-link wallet))))

(def wallet-links (links))

(fact
  (let [wallet (w/new-wallet "seed")]
    (:self (wallet-links wallet)) => (str "/" (:identity wallet))))
