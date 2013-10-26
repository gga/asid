(ns asid.test.graph-facts
  (:use midje.sweet
        asid.graph)

  (:require [asid.nodes :as an]
            [asid.calling-card :as cc]
            [asid.calling-card-repository :as ccr]
            [asid.connection-request :as cr]
            [asid.connection-request-repository :as crr]
            [asid.trust-pool :as tp]
            [asid.trust-pool-repository :as tpr]
            [asid.wallet :as w]
            [asid.wallet.repository :as wr]))

(fact "a graph starting with a wallet, then a trustpool and a calling card"
  (let [repo (an/initialize!)
        w (wr/save (w/new-wallet "seed") repo)
        p (tpr/save (tp/new-trust-pool "pool" ["name"]) repo)
        c (ccr/save (cc/new-calling-card "other-identity-uri" "other-identity"))]
    (trustpool p w)
    (adds-identity c p)
    (:identity (first (w->tps w))) => (:identity p)
    (:identity (c->w c)) => (:identity w)))

(fact "a graph starting with a wallet and then an inbound connection request"
  (let [repo (an/initialize!)
        w (wr/save (w/new-wallet "seed") repo)
        cr (crr/save (cr/new-connection-request {:from "initiator-id"
                                                 :trust {:name "pool"
                                                         :identity "pool-id"
                                                         :challenge ["a" "list"]}
                                                 :links {:initiator "uri to initiator"
                                                         :self "the calling card"}}))]
    (requests-connection cr w)
    (:identity (cr->w cr)) => (:identity w)))
