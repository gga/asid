(ns asid.graph
  (:use midje.sweet)

  (:require [asid.nodes :as an]))

(defn w->tps [wallet]
  (an/sub-objects wallet :trustpool))

(fact
  (w->tps ..wallet..) => ["trustpool"]
  (provided
    (an/sub-objects ..wallet.. :trustpool) => ["trustpool"]))
