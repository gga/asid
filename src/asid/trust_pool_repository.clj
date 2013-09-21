(ns asid.trust-pool-repository
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]
            [clojurewerkz.neocons.rest.cypher :as cy]))

(defn save [ctxt pool]
  (let [node (nn/create {:identity (:identity pool)
                         :name (:name pool)
                         :challenge (:challenge pool)})]
    (assoc pool :node-id (:id node))))
