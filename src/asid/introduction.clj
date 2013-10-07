(ns asid.introduction
  (:require [asid.render :as render]
            [asid.wallet.links :as awl]))

(defrecord Introduction [wallet])

(defn intro-to-wallet [w]
  (Introduction. w))

(extend-type Introduction
  render/Resource

  (to-json [intro]
    {:identity (-> intro :wallet :identity)
     :key {:public (-> intro :wallet :key :public)}})

  (content-type [_]
    "application/vnd.org.asidentity.introduction+json"))

(extend-type Introduction
  render/Linked

  (links [intro]
    (-> {}
        (awl/self-link (:wallet intro))
        (awl/letterplate-link (:wallet intro)))))
