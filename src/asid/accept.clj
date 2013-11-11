(ns asid.accept
  (:use midje.sweet
        [asid.error.thread :only [fail-> -log->]] )

  (:require [asid.error.definition :as ed]
            [asid.graph :as ag]
            [asid.trust-pool :as tp]
            [asid.trust-pool-repository :as tpr]
            [asid.wallet :as w]
            [asid.wallet.signing :as aws]
            [clj-http.client :as http]
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
                    (aws/packet-signer wallet)
                    json/write-str)) => truthy
    (aws/verify wallet
                (-> chal-resp :trustee-signature)
                (-> (aws/identity-packet "initiator-id")
                    (aws/packet-signer wallet)
                    json/write-str)) => truthy))

(defn- meet-challenge [conn-req wallet]
  (let [challenge-set (set (:pool-challenge conn-req))
        bag-key-set (-> wallet :bag keys set)]
    (if (set/subset? challenge-set bag-key-set)
      (new-challenge-response conn-req wallet)
      (ed/precondition-failed (str "Missing from bag: "
                                   (cs/join ", " (set/difference challenge-set
                                                                 bag-key-set)))))))

(fact "unable to meet the challenge"
  (let [conn-req {:pool-challenge ["dob"]}
        wallet {:bag {"name" "a-name"}}
        challenged (meet-challenge conn-req wallet)]
    (:status challenged) => 412
    (:message challenged) => "Missing from bag: dob"))

(defn- find-challenge-slot [cc-uri]
  (fail-> (http/get cc-uri
                    {:accept "application/vnd.org.asidentity.calling-card+json"})
          ed/http-failed?
          :body
          (json/read-str :key-fn keyword)
          (-> :links :challenge)))

(defn- submit-challenge [chal-resp conn-req wallet]
  (fail-> (find-challenge-slot (:calling-card-uri conn-req))
          (http/put {:body (json/write-str chal-resp)
                     :content-type "application/vnd.org.asidentity.challenge+json"})
          ed/http-failed?
          :body
          (json/read-str :key-fn keyword)))

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

(defn- add-trustee [handshake conn-req wallet]
  (add-trust-pool conn-req wallet))

(defn accept [conn-req wallet updates]
  (fail-> (meet-challenge conn-req wallet)
          (submit-challenge conn-req wallet)
          (add-trustee conn-req wallet)))
