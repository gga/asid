(ns asid.accept
  (:use midje.sweet
        [asid.error.thread :only [fail-> -log-> has-failed?]] )

  (:require [asid.error.definition :as ed]
            [asid.graph :as ag]
            [asid.http :as http]
            [asid.nodes :as an]
            [asid.trust-pool :as tp]
            [asid.trust-pool-repository :as tpr]
            [asid.trustee :as t]
            [asid.trustee-agent :as ta]
            [asid.trustee-repository :as tr]
            [asid.wallet :as w]
            [asid.wallet.signing :as aws]
            [clojure.data.json :as json]
            [clojure.set :as set]
            [clojure.string :as cs]
            [clojure.tools.logging :as log]))

(defn- new-challenge-response [conn-req wallet]
  (let [chal-sigs (vec (map #(aws/sign wallet
                                       (aws/data-packet % (-> wallet :bag %)))
                            (:pool-challenge conn-req)))]
    {:trustee-signature (aws/sign wallet
                                  (aws/identity-packet (:from-identity conn-req)))
     :bag (zipmap (:pool-challenge conn-req)
                  (map #(% (:bag wallet)) (:pool-challenge conn-req)))
     :bag-signature (zipmap (:pool-challenge conn-req) chal-sigs)}))

(fact
  (let [wallet (w/add-data (w/new-wallet "seed") :dob "1/1/1970")
        chal-resp (new-challenge-response {:from-identity "initiator-id"
                                           :pool-challenge [:dob]}
                                          wallet)]
    (-> chal-resp :bag :dob) => "1/1/1970"
    (aws/verify wallet
                (-> chal-resp :bag-signature :dob)
                (-> (aws/data-packet :dob "1/1/1970")
                    (aws/packet-signer (:identity wallet))
                    json/write-str)) => truthy
    (aws/verify wallet
                (-> chal-resp :trustee-signature)
                (-> (aws/identity-packet "initiator-id")
                    (aws/packet-signer (:identity wallet))
                    json/write-str)) => truthy))

(defn- meet-challenge [trustee-agent wallet]
  (let [challenge-key-set (ta/challenge trustee-agent)
        bag-key-set (-> wallet :bag keys set)]
    (if (set/subset? challenge-key-set bag-key-set)
      (new-challenge-response (ta/agent-conn-req trustee-agent) wallet)
      (ed/precondition-failed (str "Missing from bag: "
                                   (cs/join ", " (set/difference challenge-key-set
                                                                 bag-key-set)))))))

(fact "unable to meet the challenge"
  (let [conn-req {:pool-challenge ["dob"]}
        wallet {:bag {"name" "a-name"}}
        challenged (meet-challenge (ta/new-trustee-agent conn-req) wallet)]
    (:status challenged) => 412
    (:message challenged) => "Missing from bag: dob"))

(defn- find-challenge-slot [cc-uri]
  (fail-> (http/get cc-uri
                    {:accept "application/vnd.org.asidentity.calling-card+json"})
          :body
          (-> :links :challenge)))

(defn- submit-challenge [chal-resp conn-req wallet]
  (fail-> (find-challenge-slot (:calling-card-uri conn-req))
          (http/put {:body (json/write-str chal-resp)
                     :content-type "application/vnd.org.asidentity.challenge+json"})
          :body))

(defn- add-trust-pool [conn-req wallet]
  (if-let [pool (ag/w->tp wallet (:pool-identity conn-req))]
    ;; TODO: Verify that there isn't a trust pool with the same name,
    ;; but a different identity
    pool
    (let [pool (tpr/save (tp/new-trust-pool (:pool-name conn-req)
                                            (:pool-challenge conn-req)))]
      (ag/trustpool pool wallet))))

(fact
  (add-trust-pool {:pool-identity "pool-id"} ..wallet..) => ..pool..
  (provided
    (ag/w->tp ..wallet.. "pool-id") => ..pool..)

  (add-trust-pool {:pool-identity "pool id"
                   :pool-name "pool name"
                   :pool-challenge ["challenge"]}
                  ..wallet..) => ..linked-pool..
  (provided
    (ag/w->tp ..wallet.. "pool id") => nil
    (tpr/save anything) => "trust pool"
    (ag/trustpool "trust pool" ..wallet..) => ..linked-pool..))

(defn- verify-wallet [{id-sig :identity chal-sigs :challenge} conn-req wallet]
  (let [sig (w/make-signature (:identity conn-req) id-sig (:challenge chal-sigs))]
    (ag/verifies (an/create-node sig) wallet)))

(defn- connect-trustee [sigs bag pool conn-req wallet]
  (let [trustee (tr/save (t/new-trustee (:from-identity conn-req)))]
    (ag/verifies sigs trustee)
    (ag/trustee trustee pool)))

(defn- confirm-handshake [handshake conn-req]
  (let [from-key (w/parse-public-key (:from-key conn-req))
        id-packet (aws/packet-signer (aws/identity-packet (:from-identity conn-req))
                                     (:from-identity conn-req))]
    (if (aws/verify-with-key from-key
                             (-> handshake :verification :identity)
                             (json/write-str id-packet))
      handshake
      (ed/bad-request "Identity signature is invalid."))))

(facts "about confirm-handshake"
  (fact "valid"
    (let [other (w/add-data (w/new-wallet "another wallet") :name "other")]
      (confirm-handshake {:verification {:identity (aws/sign other
                                                             (aws/identity-packet (:identity other)))
                                         :challenge {:name (aws/sign other
                                                                     (aws/data-packet :name "mine"))}}}
                         {:from-identity (:identity other)
                          :from-key (-> other :key :public)})) =not=> has-failed?)

  (fact "failed handshake"
    (let [alice (w/new-wallet "trusted wallet")
          eve (w/new-wallet "attacker wallet")]
      (confirm-handshake {:verification {:identity (aws/sign eve
                                                             (aws/identity-packet (:identity alice)))}}
                         {:from-identity (:identity alice)
                          :from-key (-> alice :key :public)}) => has-failed?)))

(defn- add-trustee [{verification :verification, bag :bag} conn-req wallet]
  (let [pool (add-trust-pool conn-req wallet)]
    (fail-> (verify-wallet verification conn-req wallet)
            (connect-trustee bag pool conn-req wallet))))

(defn accept [conn-req wallet updates]
  (fail-> (ta/new-trustee-agent conn-req)
          (meet-challenge wallet)
          (submit-challenge conn-req wallet)
          (confirm-handshake conn-req)
          (add-trustee conn-req wallet)))
