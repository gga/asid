(ns asid.trust-pool-repository
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]
            [clojurewerkz.neocons.rest.cypher :as cy]
            [asid.error.definition :as ed])

  (:import [asid.trust_pool TrustPool]))

(defn save [pool ctxt]
  (let [node (nn/create {:identity (:identity pool)
                         :name (:name pool)
                         :challenge (:challenge pool)})]
    (conj pool [:node-id (:id node)])))

(defn pool-from-node [node]
  (conj (TrustPool. (-> node :data :name)
                    (-> node :data :identity)
                    (-> node :data :challenge))
        [:node-id (:id node)]))

(defn pool-from-wallet [wallet poolid]
  (let [results (cy/tquery (str "START wallet=node({walletnode}) "
                                "MATCH wallet-[:trustpool]->pool "
                                "WHERE pool.identity = {poolid} "
                                "RETURN pool")
                           {:walletnode (:node-id wallet)
                            :poolid poolid})]
    (if (not (empty? results))
      (-> results
          first
          (get "pool")
          :self
          nn/fetch-from
          pool-from-node)
      (ed/not-found))))
