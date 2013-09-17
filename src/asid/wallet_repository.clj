(ns asid.wallet-repository
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]
            [clojurewerkz.neocons.rest.cypher :as cy])

  (:import [asid.wallet Wallet]))

(defn initialize! []
  (nr/connect! "http://localhost:7474/db/data/")
  (let [root (nn/get 0)
        asid-rels (nrl/outgoing-for root :types [:asid])]
    (if (seq asid-rels)
      {:root (-> asid-rels first :end nn/fetch-from)}
      (let [asid-root (nn/create {:name "asid root"})]
        (nrl/create root asid-root :asid)
        {:root asid-root}))))

(defn attach-to-node [node link data]
  (let [data-node (nn/create data)]
        (nrl/create node data-node link)))

(defn sub-node [start type]
  (first (map #(-> % :end nn/fetch-from)
              (nrl/traverse (:id start)
                            :relationships [{:direction "out" :type type}]))))

(defn- update [wallet]
  (nn/update (:node-id wallet) {:identity (:identity wallet)})
  (let [wallet-node (nn/get (:node-id wallet))
        keys [:bag :signatures :key]]
    (doseq [[node data] (map list
                             (map #(sub-node wallet-node %) keys)
                             (map #(% wallet) keys))]
      (nn/update node data)))
  wallet)

(defn save [ctxt wallet]
  (if (:node-id wallet)
    (update wallet)
    (let [node (nn/create {:identity (:identity wallet)})]
      (doseq [data [:bag :signatures :key]]
        (attach-to-node node data (get wallet data)))
      (nrl/create (:root ctxt) node :wallet)
      wallet)))

(defn wallet-from-node [node]
  (let [bag-node (sub-node node :bag)
        sig-node (sub-node node :signatures)
        key-node (sub-node node :key)]
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
