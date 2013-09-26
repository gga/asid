(ns asid.calling-card
  (:require [asid.identity :as aid]
            [asid.neo :as an]))

(defrecord CallingCard [identity other-party])

(defn new-calling-card [other-party-identity]
  (CallingCard. (aid/new-identity other-party-identity) other-party-identity))

(defn submit [card wallet pool])

(defn attach [card pool]
  (an/connect-nodes card pool :adds-identity)
  card)

(defn uri [card]
  (str "/card" (:identity card)))

(defn self-link [so-far card]
  (conj so-far [:self (uri card)]))

(defn links [card]
  (-> {}
      (self-link card)))
