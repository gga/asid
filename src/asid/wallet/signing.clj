(ns asid.wallet.signing
  (:use midje.sweet)

  (:require [asid.strings :as as]
            [asid.wallet :as w]
            [clojure.data.json :as json])

  (:import [org.bouncycastle.jce.provider BouncyCastleProvider]
           [java.security Security Signature SecureRandom]))

(defn identity-packet [wallet-id]
  [:key :identity
   :value wallet-id])

(fact
  (let [wallet (w/new-wallet "seed")]
    (identity-packet (:identity wallet)) => [:key :identity :value (:identity wallet)]))

(defn packet-signer [packet me]
  (conj packet :signer (:identity me)))

(defn sign [me packet]
  (Security/addProvider (BouncyCastleProvider.))
  (let [sig (Signature/getInstance "ECDSA" "BC")
        packet-text (json/write-str (packet-signer packet me))]
    (.initSign sig (w/private-key me) (SecureRandom.))
    (.update sig (.getBytes packet-text "UTF-8"))
    (as/to-hex (.sign sig))))

(defn verify [me signature packet-text]
  (Security/addProvider (BouncyCastleProvider.))
  (let [sig (Signature/getInstance "ECDSA" "BC")]
    (.initVerify sig (w/public-key me))
    (.update sig (.getBytes packet-text "UTF-8"))
    (.verify sig (as/from-hex signature))))

(fact
  (let [wallet (w/new-wallet "seed")]
    (sign wallet {:value "val"}) =not=> nil?
    (verify wallet
            (sign wallet [:val "data"])
            (json/write-str [:val "data" :signer (:identity wallet)])) => truthy))

