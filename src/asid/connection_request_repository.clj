(ns asid.connection-request-repository
  (:require [asid.nodes :as an]))

(defn save [conn-req]
  (an/associate-node conn-req
                     (an/create-node conn-req)))
