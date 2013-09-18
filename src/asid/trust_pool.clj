(ns asid.trust-pool
  (:use midje.sweet)
  (:use asid.wallet))

(defrecord Origin [identity url])
(defrecord Trustee [signed-values origin])

(defrecord TrustPool [challenge trustees])

(defn new-trust-pool [req-keys]
  (TrustPool. req-keys []))

(defn add-to-pool [pool owner-wallet joiner challenge-response]
  (let [joiner-origin (Origin. (:identity joiner) (:url joiner))
        trustee (Trustee. (map #(sign owner-wallet (:identity joiner) %1 %2)
                               (:challenge pool)
                               challenge-response) joiner-origin)]
    (TrustPool. (:challenge pool)
                (conj (:trustees pool) trustee))))

(facts "about adding to a trust pool"
  (fact "should add with complete challenge"
    (-> (add-to-pool (TrustPool. [:name] [])
                     (new-wallet "id seed")
                     {:identity "id" :url "url"}
                     ["blah"]) :trustees count) => 1))
