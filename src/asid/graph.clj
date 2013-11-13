(ns asid.graph
  (:use midje.sweet)

  (:require [asid.nodes :as an]))

(defn w->tps [wallet]
  (an/children wallet :trustpool))

(fact
  (w->tps ..wallet..) => ["trustpool"]
  (provided
    (an/children ..wallet.. :trustpool) => ["trustpool"]))

(defn w->crs [wallet]
  (an/children wallet :requestsconn))

(fact
  (w->crs ..wallet..) => ["conn-req"]
  (provided
    (an/children ..wallet.. :requestsconn) => ["conn-req"]))

(defn w->ccs [wallet]
  (if (an/has-node? wallet)
    (an/nodes-by-cypher (str "START wallet=node({walletnode}) "
                             "MATCH wallet-[:trustpool]->()<-[:addsidentity]-card "
                             "RETURN card")
                        {:walletnode (an/node-from wallet)}
                        "card")))

(fact
  (w->ccs ..wallet..) => ..cards..
  (provided
    (an/has-node? ..wallet..) => true
    (an/node-from ..wallet..) => "node descriptor"
    (an/nodes-by-cypher (str "START wallet=node({walletnode}) "
                             "MATCH wallet-[:trustpool]->()<-[:addsidentity]-card "
                             "RETURN card")
                        {:walletnode "node descriptor"}
                        "card") => ..cards..))

(defn w->tp [wallet pool-id]
  (an/node-with-identity wallet :trustpool pool-id))

(fact
  (w->tp ..wallet.. "pool-id") => ..trust-pool..
  (provided
    (an/node-with-identity ..wallet.. :trustpool "pool-id") => ..trust-pool..))

(defn tp->t [pool trustee-id]
  (an/node-with-identity pool :trust trustee-id))

(fact
  (tp->t ..pool.. "trustee-id") => ..trustee..
  (provided
    (an/node-with-identity ..pool.. :trust "trustee-id") => ..trustee..))

(defn c->w [card]
  (-> (an/child card :addsidentity)
      (an/superior :trustpool)))

(fact
  (c->w ..card..) => "wallet"
  (provided
    (an/child ..card.. :addsidentity) => ..pool..
    (an/superior ..pool.. :trustpool) => "wallet"))

(defn c->tp [card]
  (an/child card :addsidentity))

(defn tp->w [pool]
  (an/superior pool :trustpool))

(fact
  (tp->w ..pool..) => "wallet"
  (provided
    (an/superior ..pool.. :trustpool) => "wallet"))

(defn cr->w [conn-req]
  (an/superior conn-req :requestsconn))

(fact
  (cr->w ..conn-req..) => "wallet"
  (provided
    (an/superior ..conn-req.. :requestsconn) => "wallet"))

(defn trustpool [pool wallet]
  (an/connect-nodes wallet pool :trustpool)
  pool)

(defn adds-identity [card pool]
  (an/connect-nodes card pool :addsidentity)
  card)

(defn requests-connection [conn-req wallet]
  (an/connect-nodes wallet conn-req :requestsconn)
  conn-req)

(defn verifies [sig wallet]
  (an/connect-nodes wallet sig :verifies)
  sig)
