(ns asid.wallet.signing
  (:use midje.sweet)

  (:require [asid.strings :as as]
            [asid.wallet :as w]
            [clojure.data.json :as json])

  (:import [org.bouncycastle.jce.provider BouncyCastleProvider]
           [java.security Security Signature SecureRandom]))

(defn sign [me other-id key value]
  (Security/addProvider (BouncyCastleProvider.))
  (let [sig (Signature/getInstance "ECDSA" "BC")
        packet (json/write-str {:signer (-> me :identity)
                                :trustee other-id
                                :key key
                                :value value})]
    (.initSign sig (w/private-key me) (SecureRandom.))
    (.update sig (.getBytes packet "UTF-8"))
    (as/to-hex (.sign sig))))

