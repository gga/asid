(ns asid.connection-request-repository
  (:require [clojurewerkz.neocons.rest.nodes :as nn]))

(defn save [conn-req]
  (let [node (nn/create conn-req)]
    (conj conn-req [:node-id (:id node)])))
