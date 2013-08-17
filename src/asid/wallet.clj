(ns asid.wallet
  (:use midje.sweet)
  (:use asid.strings)

  (:import [org.bouncycastle.jce.provider BouncyCastleProvider]
           [java.security Security KeyPairGenerator SecureRandom]
           [java.security.spec ECGenParameterSpec]))

(defn elliptic-key-pair []
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
    (:public (elliptic-key-pair)) => #"^0x[0-9a-f]+")
  (fact "private key should be a hex encoded string"
    (:private (elliptic-key-pair)) => #"^0x[0-9a-f]+"))

(defrecord Wallet [bag signatures key])

(defn new-wallet []
  (Wallet. {} {} (elliptic-key-pair)))

(facts "about new-wallet"
  (fact "should have a key"
    (-> (new-wallet) :key) =not=> nil?)
  (fact "should have a public key"
    (-> (new-wallet) :key :public) =not=> nil?)
  (fact "should have a private key"
    (-> (new-wallet) :key :private) =not=> nil?))
