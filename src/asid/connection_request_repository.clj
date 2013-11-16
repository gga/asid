(ns asid.connection-request-repository
  (:use [asid.error.thread :only [fail->]])

  (:require [asid.error.definition :as ed]
            [asid.nodes :as an])

  (:import [asid.connection_request ConnectionRequest]))

(defn save [conn-req]
  (an/associate-node conn-req
                     (an/create-node conn-req)))

(defn conn-req-from-node [node]
  (an/associate-node (ConnectionRequest. (:identity node)
                                         (:from-identity node)
                                         (:from-key node)
                                         (:pool-name node)
                                         (:pool-identity node)
                                         (map keyword (:pool-challenge node))
                                         (:initiator-uri node)
                                         (:calling-card-uri node))
                     node))

(defn- find-conn-req [wallet conn-req-id]
  (if-let [cr (an/node-with-identity wallet :requestsconn conn-req-id)]
    cr
    (ed/not-found)))

(defn conn-req-from-wallet [wallet conn-req-id]
  (fail-> (find-conn-req wallet conn-req-id)
          conn-req-from-node))
