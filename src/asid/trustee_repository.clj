(ns asid.trustee-repository
  (:use [asid.error.thread :only [fail->]])

  (:require [asid.error.definition :as ed]
            [asid.graph :as ag]))

(defn trustee-from-node [node])

(defn- find-trustee [pool trustee-id]
  (if-let [trustee (ag/tp->t pool trustee-id)]
    trustee
    (ed/not-found)))

(defn trustee-in-pool [pool trustee-id]
  (fail-> (find-trustee pool trustee-id)
          trustee-from-node))
