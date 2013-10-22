(ns asid.trust-pool-repository
  (:use [asid.error.thread :only [fail->]])

  (:require [asid.nodes :as an])

  (:import [asid.trust_pool TrustPool]))

(defn save [pool ctxt]
  (an/associate-node pool
                     (an/create-node {:identity (:identity pool)
                                      :name (:name pool)
                                      :challenge (:challenge pool)})))

(defn pool-from-node [node]
  (an/associate-node (TrustPool. (-> node :name)
                                 (-> node :identity)
                                 (-> node :challenge))
                     node))

(defn pool-from-wallet [wallet poolid]
  (fail-> (an/node-with-identity wallet :trustpool poolid)
          pool-from-node))
