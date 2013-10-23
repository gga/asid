(ns asid.graph
  (:use midje.sweet)

  (:require [asid.nodes :as an]))

(defn w->tps [wallet]
  (an/children wallet :trustpool))

(fact
  (w->tps ..wallet..) => ["trustpool"]
  (provided
    (an/children ..wallet.. :trustpool) => ["trustpool"]))
