(ns asid.strings
  (:use midje.sweet)

  (:require [clojure.data.codec.base64 :as b64])

  (:import [java.net URI]))

(defn to-hex [arr]
  (String. (b64/encode arr) "UTF-8"))

(facts "to-hex"
  (to-hex (.getBytes "   ")) => "ICAg"
  (to-hex (.getBytes "")) => "")

(defn from-hex [data-str]
  (b64/decode (.getBytes data-str)))

(facts "from-hex"
  (seq (from-hex "ICAg")) => (seq (bytes (byte-array (map (comp byte int) "   "))))
  (seq (from-hex (to-hex (.getBytes "hello, world!")))) => (seq (bytes (byte-array (map (comp byte int) "hello, world!")))))

(defn resolve-url [rel orig]
  (-> (URI. orig)
    (.resolve rel)
    .toString))

(fact
  (resolve-url "/hello" "http://localhost:8888/hfdf-45fd") => "http://localhost:8888/hello")
