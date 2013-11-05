(ns asid.trust-pool-repository
  (:use [asid.error.thread :only [fail->]])

  (:require [asid.error.definition :as ed]
            [asid.nodes :as an])

  (:import [asid.trust_pool TrustPool]))

(defn save [pool]
  (an/associate-node pool
                     (an/create-node {:identity (:identity pool)
                                      :name (:name pool)
                                      :challenge (:challenge pool)})))

(defn pool-from-node [node]
  (an/associate-node (TrustPool. (-> node :name)
                                 (-> node :identity)
                                 (-> node :challenge))
                     node))

(defn- find-pool [wallet pool-id]
  (if-let [pool (an/node-with-identity wallet :trustpool pool-id)]
    pool
    (ed/not-found)))

(defn pool-from-wallet [wallet poolid]
  (fail-> (find-pool wallet poolid)
          pool-from-node))
