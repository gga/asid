(ns asid.calling-card-repository
  (:require [asid.error.definition :as ed]
            [asid.nodes :as an])

  (:import [asid.calling_card CallingCard]))

(defn save [card]
  (an/associate-node card
                     (an/create-node card)))

(defn card-from-node [node]
  (an/associate-node (CallingCard. (-> node :identity)
                                   (-> node :target-uri)
                                   (-> node :other-party))
                     node))

(defn card-from-wallet [wallet card-id]
  (let [results (an/nodes-by-cypher (str "START wallet=node({walletnode}) "
                                         "MATCH wallet-[:trustpool]->()<-[:addsidentity]-card "
                                         "WHERE card.identity = {cardid} "
                                         "RETURN card")
                                    {:walletnode (an/node-from wallet)
                                     :cardid card-id}
                                    "card")]
    (if (not (empty? results))
      (card-from-node (first results))
      (ed/not-found))))
