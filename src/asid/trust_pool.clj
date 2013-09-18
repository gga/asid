(ns asid.trust-pool
  (:use midje.sweet)
  (:require [asid.wallet :as w]
            [asid.identity :as aid]))

(defrecord Origin [identity url])
(defrecord Trustee [signed-values origin])

(defrecord TrustPool [name identity challenge trustees])

(defn new-trust-pool [name req-keys]
  (TrustPool. name (aid/new-identity name) req-keys []))

(defn add-to-pool [pool owner-wallet joiner challenge-response]
  (let [joiner-origin (Origin. (:identity joiner) (:url joiner))
        trustee (Trustee. (map #(w/sign owner-wallet (:identity joiner) %1 %2)
                               (:challenge pool)
                               challenge-response) joiner-origin)]
    (TrustPool. (:name pool)
                (:identity pool)
                (:challenge pool)
                (conj (:trustees pool) trustee))))

(facts "about adding to a trust pool"
  (fact "should add with complete challenge"
    (-> (add-to-pool (TrustPool. "name" "id" [:name] [])
                     (w/new-wallet "id seed")
                     {:identity "id" :url "url"}
                     ["blah"]) :trustees count) => 1))
