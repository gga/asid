(ns asid.wallet
  (:use midje.sweet)
  (:use asid.strings)

  (:require [clojure.data.json :as json]
            [asid.identity :as aid]
            [asid.neo :as an])

  (:import [org.bouncycastle.jce.provider BouncyCastleProvider]
           [java.security KeyFactory Security KeyPairGenerator SecureRandom Signature]
           [java.security.spec ECGenParameterSpec PKCS8EncodedKeySpec]))

(defn new-key-pair []
  (Security/addProvider (BouncyCastleProvider.))
  (let [ecGenSpec (ECGenParameterSpec. "prime192v1")
        keyGen (KeyPairGenerator/getInstance "ECDSA" "BC")]
    (.initialize keyGen ecGenSpec (SecureRandom/getInstance "SHA1PRNG"))
    (let [pair (.generateKeyPair keyGen)]
      {:public (to-hex (-> pair .getPublic .getEncoded))
       :private (to-hex (-> pair .getPrivate .getEncoded))})))

(fact "about key pair"
  (fact "public key should be a hex encoded string"
    (:public (new-key-pair)) => #"^[\d\w+/]+$")
  (fact "private key should be a hex encoded string"
    (:private (new-key-pair)) => #"^[\d\w+/]+=$"))

(defrecord Wallet [identity bag signatures key])

(defn new-wallet [id-seed]
  (Wallet. (aid/new-identity id-seed) {} {} (new-key-pair)))

(facts "about new-wallet"
  (fact "should have a key"
    (-> (new-wallet "seed") :key) =not=> nil?)
  (fact "should have a public key"
    (-> (new-wallet "seed") :key :public) =not=> nil?)
  (fact "should have a private key"
    (-> (new-wallet "seed") :key :private) =not=> nil?)
  (fact "should have an identity"
    (-> (new-wallet "seed") :identity) =not=> nil?))

(defn uri [wallet]
  (str "/" (-> wallet :identity)))

(fact
  (uri (Wallet. "fake-id" {} {} (new-key-pair))) => "/fake-id")

(defn bag-uri [wallet]
  (str (uri wallet) "/bag"))

(defn trustpool-uri [wallet]
  (str (uri wallet) "/trustpool"))

(defn letterplate-uri [wallet]
  (str (uri wallet) "/letterplate"))

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

(defn add-data [wallet key value]
  (assoc (Wallet. (:identity wallet)
                  (assoc (:bag wallet) key value)
                  (:signatures wallet)
                  (:key wallet))
    :node-id (:node-id wallet)))

(fact
  (let [orig (new-wallet "id")
        added (add-data orig :item "value")]
    (-> added :bag :item) => "value"
    (-> added :identity) => (-> orig :identity)
    (-> added :key :public) => (-> orig :key :public)))
