(ns asid.connection-request
  (:require [asid.neo :as an]))

(defrecord ConnectionRequest [from-identity
                              pool-name
                              pool-identity
                              pool-challenge
                              initiator-uri
                              calling-card-uri])

(defn new-connection-request [data]
  (ConnectionRequest. (-> data :from)
                      (-> data :trust :name)
                      (-> data :trust :identity)
                      (-> data :trust :challenge)
                      (-> data :links :initiator)
                      (-> data :links :self)))

(defn attach [conn-req wallet]
  (an/connect-nodes conn-req wallet :requests-conn)
  conn-req)
