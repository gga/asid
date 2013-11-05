(ns asid.wallet.links
  (:use midje.sweet)

  (:require [asid.graph :as ag]
            [asid.trust-pool :as tp]
            [asid.calling-card :as cc]
            [asid.connection-request :as cr]
            [asid.wallet :as w]
            [asid.render :as render]

            [asid.nodes :as an]
            [asid.wallet.repository :as wr]
            [asid.trust-pool-repository :as tpr]
            [asid.calling-card-repository :as ccr])

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
                                 (ag/w->tps wallet))]))

(defn all-calling-card-links [so-far wallet]
  (conj so-far [:cards (map #(cc/uri % wallet)
                            (ag/w->ccs wallet))]))

(defn all-connection-request-links [so-far wallet]
  (conj so-far [:connectionRequests (map #(cr/uri % wallet)
                                         (ag/w->crs wallet))]))

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
        (all-calling-card-links wallet)
        (all-connection-request-links wallet)
        (letterplate-link wallet))))

(fact
  (let [repo (an/initialize!)] 
    (let [tw (wr/save (w/new-wallet "id") repo)
          pool (tpr/save (tp/new-trust-pool "named" ["dob"]))
          card (ccr/save (cc/new-calling-card "other-uri" "other-id"))]
      (ag/trustpool pool tw)
      (ag/adds-identity card pool)

      (render/links tw) => (contains {:bag anything})
      (render/links tw) => (contains {:trustpool anything})
      (render/links tw) => (contains {:trustpools (contains anything)})
      (render/links tw) => (contains {:cards (contains anything)}))
    
    (let [wallet (w/new-wallet "seed")]
      (:self (render/links wallet)) => (str "/" (:identity wallet)))))
