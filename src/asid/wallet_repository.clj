(ns asid.wallet-repository
  (:require [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]
            [clojurewerkz.neocons.rest.cypher :as cy]
            [asid.neo :as an])

  (:import [asid.wallet Wallet]))

(defn- update [wallet]
  (nn/update (:node-id wallet) {:identity (:identity wallet)})
  (let [wallet-node (nn/get (:node-id wallet))
        keys [:bag :signatures :key]]
    (doseq [[node data] (map list
                             (map #(an/sub-node wallet-node %) keys)
                             (map #(% wallet) keys))]
      (nn/update node data)))
  wallet)

(defn save [ctxt wallet]
  (if (:node-id wallet)
    (update wallet)
    (let [node (nn/create {:identity (:identity wallet)})]
      (doseq [data [:bag :signatures :key]]
        (an/attach-to-node node data (get wallet data)))
      (nrl/create (:root ctxt) node :wallet)
      wallet)))

(defn wallet-from-node [node]
  (let [bag-node (an/sub-node node :bag)
        sig-node (an/sub-node node :signatures)
        key-node (an/sub-node node :key)]
    (assoc (Wallet. (-> node :data :identity)
                    (:data bag-node)
                    (:data sig-node)
                    (:data key-node))
      :node-id (:id node))))

(defn get-wallet [ctxt id]
  (-> (cy/tquery (str "START asid=node(1) "
                      "MATCH asid-[:wallet]->wallet "
                      "WHERE wallet.identity = {walletid} "
                      "RETURN wallet")
                 {:root (-> ctxt :root :id)
                  :walletid id})
      first
      (get "wallet")
      :self
      nn/fetch-from
      wallet-from-node))
