(ns asid.graph
  (:use midje.sweet)

  (:require [asid.nodes :as an]))

(defn w->tps [wallet]
  (an/children wallet :trustpool))

(fact
  (w->tps ..wallet..) => ["trustpool"]
  (provided
    (an/children ..wallet.. :trustpool) => ["trustpool"]))

(defn w->ccs [wallet]
  (an/wallet-to-cards wallet))

(fact
  (w->ccs ..wallet..) => ..cards..
  (provided
    (an/wallet-to-cards ..wallet..) => ..cards..))

(defn c->w [card]
  (-> (an/child card :addsidentity)
      (an/superior :trustpool)))

(fact
  (c->w ..card..) => "wallet"
  (provided
    (an/child ..card.. :addsidentity) => ..pool..
    (an/superior ..pool.. :trustpool) => "wallet"))

(defn tp->w [pool]
  (an/superior pool :trustpool))

(fact
  (tp->w ..pool..) => "wallet"
  (provided
    (an/superior ..pool.. :trustpool) => "wallet"))

(defn cr->w [conn-req]
  (an/child conn-req :requests-conn))

(fact
  (cr->w ..conn-req..) => "wallet"
  (provided
    (an/child ..conn-req.. :requests-conn) => "wallet"))

(defn trustpool [pool wallet]
  (an/connect-nodes wallet pool :trustpool)
  pool)

(defn adds-identity [card pool]
  (an/connect-nodes card pool :addsidentity)
  card)

(defn requests-connection [conn-req wallet]
  (an/connect-nodes conn-req wallet :requests-conn)
  conn-req)
