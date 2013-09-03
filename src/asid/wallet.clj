(ns asid.wallet
  (:use midje.sweet)
  (:use asid.strings)

  (:require [clojure.data.json :as json])

  (:import [org.bouncycastle.jce.provider BouncyCastleProvider]
           [java.security KeyFactory Security KeyPairGenerator SecureRandom Signature]
           [java.security.spec ECGenParameterSpec PKCS8EncodedKeySpec]))


(defn new-key-pair []
  (Security/addProvider (BouncyCastleProvider.))
  (let [ecGenSpec (ECGenParameterSpec. "prime192v1")
        keyGen (KeyPairGenerator/getInstance "ECDSA" "BC")]
    (.initialize keyGen ecGenSpec (SecureRandom.))
    (let [pair (.generateKeyPair keyGen)]
      {:public (to-hex (-> pair .getPublic .getEncoded))
       :private (to-hex (-> pair .getPrivate .getEncoded))})))

(fact "about key pair"
  (fact "public key should be a hex encoded string"
    (:public (new-key-pair)) => #"^[\d\w+/]+$")
  (fact "private key should be a hex encoded string"
    (:private (new-key-pair)) => #"^[\d\w+/]+=$"))

(defrecord Wallet [identity bag signatures key])

(defn new-wallet []
  (Wallet. (uuid) {} {} (new-key-pair)))

(facts "about new-wallet"
  (fact "should have a key"
    (-> (new-wallet) :key) =not=> nil?)
  (fact "should have a public key"
    (-> (new-wallet) :key :public) =not=> nil?)
  (fact "should have a private key"
    (-> (new-wallet) :key :private) =not=> nil?)
  (fact "should have an identity"
    (-> (new-wallet) :identity) => #"[0-9a-f]+-[0-9a-f]+-[0-9a-f]+-[0-9a-f]+-[0-9a-f]+"))

(defn uri [wallet]
  (str "/" (-> wallet :identity)))

(fact
  (uri (Wallet. "fake-id" {} {} (new-key-pair))) => "/fake-id")

(defn private-key [wallet]
  (let [factory (KeyFactory/getInstance "ECDSA" "BC")]
    (.generatePrivate factory (PKCS8EncodedKeySpec. (from-hex (-> wallet :key :private))))))

(defn sign [me other-id key value]
  (Security/addProvider (BouncyCastleProvider.))
  (let [sig (Signature/getInstance "ECDSA" "BC")
        packet (json/write-str {:signer (-> me :identity)
                                :trustee other-id
                                :key key
                                :value value})]
    (.initSign sig (private-key me) (SecureRandom.))
    (.update sig (.getBytes packet "UTF-8"))
    (to-hex (.sign sig))))

(defn to-json [wallet]
  {:identity (:identity wallet)
   :bag (:bag wallet)
   :signatures (:signatures wallet)
   :key {:public (-> wallet :key :public)}})

(fact
  (let [tw (Wallet. "id" {} {} {:public "pub-key" :private "priv-key"})]
    (to-json tw) => (contains {:identity "id"})
    (to-json tw) => (contains {:key (contains {:public "pub-key"})})
    {:key (to-json tw)} =not=> (contains {:private "priv-key"})))
