(ns asid.wallet
  (:use midje.sweet)
  (:use asid.strings)

  (:require [clojure.data.json :as json])

  (:import [org.bouncycastle.jce.provider BouncyCastleProvider]
           [java.security KeyFactory Security KeyPairGenerator SecureRandom Signature]
           [java.security.spec ECGenParameterSpec PKCS8EncodedKeySpec]))


(defn new-key-pair []
  (do
    (Security/addProvider (BouncyCastleProvider.))
    (let [ecGenSpec (ECGenParameterSpec. "prime192v1")
          keyGen (KeyPairGenerator/getInstance "ECDSA" "BC")]
      (do
        (.initialize keyGen ecGenSpec (SecureRandom.))
        (let [pair (.generateKeyPair keyGen)]
          {:public (to-hex (-> pair .getPublic .getEncoded))
           :private (to-hex (-> pair .getPrivate .getEncoded))})))))

(fact "about key pair"
  (fact "public key should be a hex encoded string"
    (:public (new-key-pair)) => #"^[\d\w+/]+$")
  (fact "private key should be a hex encoded string"
    (:private (new-key-pair)) => #"^[\d\w+/]+=$"))

(defrecord Wallet [bag signatures key])

(defn new-wallet []
  (Wallet. {:identity (uuid)} {} (new-key-pair)))

(facts "about new-wallet"
  (fact "should have a key"
    (-> (new-wallet) :key) =not=> nil?)
  (fact "should have a public key"
    (-> (new-wallet) :key :public) =not=> nil?)
  (fact "should have a private key"
    (-> (new-wallet) :key :private) =not=> nil?)
  (fact "should have an identity"
    (-> (new-wallet) :bag :identity) => #"[0-9a-f]+-[0-9a-f]+-[0-9a-f]+-[0-9a-f]+-[0-9a-f]+"))

(defn private-key [wallet]
  (let [factory (KeyFactory/getInstance "ECDSA" "BC")]
    (.generatePrivate factory (PKCS8EncodedKeySpec. (from-hex (-> wallet :key :private))))))

(defn sign [me other-id key value]
  (do
    (Security/addProvider (BouncyCastleProvider.))
    (let [sig (Signature/getInstance "ECDSA" "BC")
          packet (json/write-str {:signer (-> me :bag :identity)
                                  :trustee other-id
                                  :key key
                                  :value value})]
      (do
        (.initSign sig (private-key me) (SecureRandom.))
        (.update sig (.getBytes packet "UTF-8"))
        (to-hex (.sign sig))))))
