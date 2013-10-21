(ns asid.calling-card-repository
  (:require [asid.nodes :as an]))

(defn save [card]
  (an/associate-node card
                     (an/create-node card)))
