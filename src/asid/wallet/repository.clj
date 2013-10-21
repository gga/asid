(ns asid.wallet.repository
  (:require [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]
            [clojurewerkz.neocons.rest.cypher :as cy]
            [asid.nodes :as an]
            [asid.error.definition :as ed])

  (:import [asid.wallet Wallet]))

(defn wallet-from-node [node]
  (let [bag-node (an/sub-node node :bag)
        sig-node (an/sub-node node :signatures)
        key-node (an/sub-node node :key)]
    (assoc (Wallet. (-> node :data :identity)
                    (:data bag-node)
                    (:data sig-node)
                    (:data key-node))
      :node-id (:id node))))

(defn- update [wallet]
  (nn/update (:node-id wallet) {:identity (:identity wallet)})
  (let [wallet-node (nn/get (:node-id wallet))
        keys [:bag :signatures :key]]
    (doseq [[node data] (map list
                             (map #(an/sub-node wallet-node %) keys)
                             (map #(% wallet) keys))]
      (nn/update node data)))
  wallet)

(defn save [wallet ctxt]
  (if (an/has-node? wallet)
    (update wallet)
    (let [wallet (an/associate-node wallet
                                    (an/create-node {:identity (:identity wallet)}))]
      (doseq [data [:bag :signatures :key]]
        (an/attach-to-node wallet data (get wallet data)))
      (nrl/create (:root ctxt) (:node-id wallet) :wallet)
      wallet)))

(defn get-wallet [id ctxt]
  (let [results (cy/tquery (str "START asid=node({root}) "
                                "MATCH asid-[:wallet]->wallet "
                                "WHERE wallet.identity = {walletid} "
                                "RETURN wallet")
                           {:root (-> ctxt :root :id)
                            :walletid id})]
    (if (not (empty? results))
      (-> results
         first
         (get "wallet")
         :self
         nn/fetch-from
         wallet-from-node)
      (ed/not-found))))
