(ns asid.identity
  (:use midje.sweet)

  (:require [clojure.string :as cs])

  (:import [java.security SecureRandom MessageDigest]))

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

(def grammar #"([a-f0-9]{4,4}-)+[a-f0-9]{1,4}")

(defn new-identity [id-seed]
  (cs/join "-" (map (partial apply str)
                    (partition 4 (sha id-seed (salt 20))))))

(facts "about new-identity"
  (fact "should generate an alpha-numeric string as the identity"
    (new-identity "seed") => #"[a-z0-9-]+")
  (fact "an identity should be easy to read"
    (new-identity "seed") => grammar)
  (fact "identity should not be the seed"
    (new-identity "seed") =not=> "seed")
  (fact "seed should not result in the same identity"
    (new-identity "seed") =not=> (new-identity "seed")))

