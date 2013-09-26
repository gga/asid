(ns asid.calling-card-repository
  (:require [clojurewerkz.neocons.rest.nodes :as nn]))

(defn save [card]
  (let [node (nn/create card)]
    (conj card [:node-id (:id node)])))
