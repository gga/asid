(ns asid.wallet.signing
  (:use midje.sweet)

  (:require [asid.strings :as as]
            [asid.wallet :as w]
            [clojure.data.json :as json])

  (:import [org.bouncycastle.jce.provider BouncyCastleProvider]
           [java.security Security Signature SecureRandom]))

(defn identity-packet [wallet]
  {:key :identity
   :value (:identity wallet)})

(fact
  (let [wallet (w/new-wallet "seed")]
    (identity-packet wallet) => {:key :identity :value (:identity wallet)}))

(defn sign [me packet]
  (Security/addProvider (BouncyCastleProvider.))
  (let [sig (Signature/getInstance "ECDSA" "BC")
        packet-text (json/write-str (conj packet [:signer (-> me :identity)]))]
    (.initSign sig (w/private-key me) (SecureRandom.))
    (.update sig (.getBytes packet-text "UTF-8"))
    (as/to-hex (.sign sig))))

(fact
  (let [wallet (w/new-wallet "seed")]
    (sign wallet {:value "val"}) =not=> nil?))

