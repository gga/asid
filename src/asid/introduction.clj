(ns asid.introduction
  (:require [asid.render :as render]
            [asid.wallet.links :as awl]
            [asid.wallet.signing :as aws]))

(defrecord Introduction [wallet])

(defn intro-to-wallet [w]
  (Introduction. w))

(extend-type Introduction
  render/Resource

  (to-json [intro]
    (let [wallet (:wallet intro)]
      {:identity (-> wallet :identity)
       :key {:public (-> wallet :key :public)}
       :signatures {:identity (aws/sign wallet (aws/identity-packet wallet))}}))

  (content-type [_]
    "application/vnd.org.asidentity.introduction+json"))

(extend-type Introduction
  render/Linked

  (links [intro]
    (-> {}
        (awl/self-link (:wallet intro))
        (awl/letterplate-link (:wallet intro)))))
