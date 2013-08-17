(ns asid.strings
  (:use midje.sweet))

(defn to-hex [arr]
  (str "0x" (apply str (map #(format "%02x" (int %)) arr))))

(facts "to-hex"
  (to-hex "   ") => "0x202020")

