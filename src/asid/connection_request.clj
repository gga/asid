(ns asid.connection-request
  (:require [asid.nodes :as an]
            [asid.render :as render]
            [asid.identity :as aid]
            [asid.wallet :as w]))

(defrecord ConnectionRequest [identity
                              from-identity
                              pool-name
                              pool-identity
                              pool-challenge
                              initiator-uri
                              calling-card-uri])

(defn uri [conn-req wallet]
  (str (w/uri wallet) "/request/" (:identity conn-req)))

(defn new-connection-request [data]
  (ConnectionRequest. (aid/new-identity (-> data :from))
                      (-> data :from)
                      (-> data :trust :name)
                      (-> data :trust :identity)
                      (-> data :trust :challenge)
                      (-> data :links :initiator)
                      (-> data :links :self)))

(defn attach [conn-req wallet]
  (an/connect-nodes conn-req wallet :requests-conn)
  conn-req)

(extend-type ConnectionRequest
  render/Linked

  (links [conn-req]
    {:self (uri conn-req
                (an/superior conn-req :requests-conn))
     :from (:initiator-uri conn-req)
     :calling-card (:calling-card-uri conn-req)}))

(extend-type ConnectionRequest
  render/Resource

  (to-json [conn-req]
    {:identity (:identity conn-req)
     :pool {:name (:pool-name conn-req)
            :identity (:pool-identity conn-req)
            :challenge (:pool-challenge conn-req)}})

  (content-type [_]
    "application/vnd.org.asidentity.connection-request+json"))
