(ns asid.trustee-agent
  (:use midje.sweet)
  
  (:require [asid.connection-request :as cr]))

(defn new-trustee-agent [conn-req]
  {:conn-req conn-req})

(fact
  (new-trustee-agent {:conn "request"}) =not=> nil?)

(defn agent-conn-req [agent]
  (:conn-req agent))

(fact
  (-> "conn-req" new-trustee-agent agent-conn-req) => "conn-req")

(defn challenge [agent]
  (-> agent agent-conn-req :pool-challenge set))

(defn- sample-conn-req
  ([] (sample-conn-req {}))
  ([args]
     (let [defaults {:from "other identity"
                     :key "other public key"
                     :trust {:name "a trust pool"
                             :identity "trust pool id"
                             :challenge ["sample" "challenge"]}
                     :links {:initiator "initiator uri"
                             :self "conn req uri"}}]
       (cr/new-connection-request (merge defaults args)))))

(fact
  (-> (sample-conn-req)
      new-trustee-agent
      challenge) => #{:sample :challenge})
