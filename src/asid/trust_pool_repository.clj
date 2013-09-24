(ns asid.trust-pool-repository
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]
            [clojurewerkz.neocons.rest.cypher :as cy])

  (:import [asid.trust_pool TrustPool]))

(defn save [ctxt pool]
  (let [node (nn/create {:identity (:identity pool)
                         :name (:name pool)
                         :challenge (:challenge pool)})]
    (assoc pool :node-id (:id node))))

(defn pool-from-node [node]
  (assoc (TrustPool. (-> node :data :name)
                     (-> node :data :identity)
                     (-> node :data :challenge))
    :node-id (:id node)))

(defn pool-from-wallet [wallet poolid]
  (-> (cy/tquery (str "START wallet=node({walletnode}) "
                      "MATCH wallet-[:trustpool]->pool "
                      "WHERE pool.identity = {poolid} "
                      "RETURN pool")
                 {:walletnode (:node-id wallet)
                  :poolid poolid})
      first
      (get "pool")
      :self
      nn/fetch-from
      pool-from-node))
