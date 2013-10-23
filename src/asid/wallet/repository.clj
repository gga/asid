(ns asid.wallet.repository
  (:use [asid.error.thread :only [fail->]])

  (:require [asid.nodes :as an]
            [asid.error.definition :as ed])

  (:import [asid.wallet Wallet]))

(defn wallet-from-node [node]
  (an/associate-node (Wallet. (-> node :identity)
                              (an/child node :bag)
                              (an/child node :signatures)
                              (an/child node :key))
                     node))

(defn- update [wallet]
  (an/update-node wallet {:identity (:identity wallet)})
  (let [keys [:bag :signatures :key]]
    (doseq [[node data] (map list
                             (map #(an/child wallet %) keys)
                             (map #(% wallet) keys))]
      (an/update-node node data)))
  wallet)

(defn save [wallet ctxt]
  (if (an/has-node? wallet)
    (update wallet)
    (let [wallet (an/associate-node wallet
                                    (an/create-node {:identity (:identity wallet)}))]
      (doseq [data [:bag :signatures :key]]
        (an/attach-to-node wallet data (get wallet data)))
      (an/connect-nodes ctxt wallet :wallet)
      wallet)))

(defn get-wallet [id ctxt]
  (fail-> (an/node-with-identity ctxt :wallet id)
          wallet-from-node))
