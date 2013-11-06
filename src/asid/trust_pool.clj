(ns asid.trust-pool
  (:use midje.sweet)

  (:require [asid.graph :as ag]
            [asid.identity :as aid]
            [asid.render :as render]
            [asid.wallet :as w]
            [asid.wallet.signing :as aws]))

(defrecord TrustPool [name identity challenge])

(defn new-trust-pool
  ([name req-keys] (new-trust-pool (aid/new-identity name)
                                   name
                                   req-keys))
  ([identity name req-keys] (TrustPool. name identity req-keys)))

(defn uri [wallet pool]
  (str (w/uri wallet) "/trustpool/" (:identity pool)))

(defrecord Origin [identity url])
(defrecord Trustee [signed-values origin])

(defn add-to-pool [pool owner-wallet joiner challenge-response]
  (let [joiner-origin (Origin. (:identity joiner) (:url joiner))]
    (Trustee. (map #(aws/sign owner-wallet (:identity joiner) %1 %2)
                   (:challenge pool)
                   challenge-response) joiner-origin)))

(fact "should add with complete challenge"
  (add-to-pool (TrustPool. "name" "id" [:name])
               (w/new-wallet "id seed")
               {:identity "id" :url "url"}
               "blah") =not=> nil?)

(defn self-link [so-far pool]
  (conj so-far [:self (uri (ag/tp->w pool) pool)]))

(extend-type TrustPool
  render/Resource

  (to-json [pool]
    {:name (:name pool)
     :identity (:identity pool)
     :challenge (:challenge pool)})

  (content-type [_]
    "application/vnd.org.asidentity.trust-pool+json"))

(extend-type TrustPool
  render/Linked

  (links [pool]
    (-> {}
        (self-link pool))))
