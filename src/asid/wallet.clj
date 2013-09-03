(ns asid.wallet
  (:use midje.sweet)
  (:use asid.strings)

  (:require [clojure.data.json :as json]
            [clojure.string :as cs])

  (:import [org.bouncycastle.jce.provider BouncyCastleProvider]
           [java.security KeyFactory Security KeyPairGenerator SecureRandom Signature MessageDigest]
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

(defn- salt [length]
  (let [rand (SecureRandom/getInstance "SHA1PRNG")
        salt-bytes (make-array Byte/TYPE length)]
    (.nextBytes rand salt-bytes)
    salt-bytes))

(defn- sha [message salt]
  (apply str
         (map (partial format "%02x")
              (.digest (doto (MessageDigest/getInstance "SHA-1")
                         .reset
                         (.update (byte-array (mapcat seq [(.getBytes message) salt]))))))))

(def wallet-identity-grammar #"([a-f0-9]{4,4}-)+[a-f0-9]{1,4}")

(defn- new-identity [id-seed]
  (cs/join "-" (map (partial apply str)
                    (partition 4 (sha id-seed (salt 20))))))

(facts "about new-identity"
  (fact "should generate an alpha-numeric string as the identity"
    (new-identity "seed") => #"[a-z0-9-]+")
  (fact "an identity should be easy to read"
    (new-identity "seed") => wallet-identity-grammar)
  (fact "identity should not be the seed"
    (new-identity "seed") =not=> "seed")
  (fact "seed should not result in the same identity"
    (new-wallet "seed") =not=> (new-wallet "seed")))

(defn new-wallet [id-seed]
  (Wallet. (new-identity id-seed) {} {} (new-key-pair)))

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
