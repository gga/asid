(ns asid.accept
  (:use midje.sweet
        [asid.error.thread :only [fail->]] )

  (:require [asid.error.definition :as ed]
            [asid.graph :as ag]
            [asid.trust-pool :as tp]
            [asid.trust-pool-repository :as tpr]
            [clojure.set :as set]
            [clojure.string :as cs]
            [clojure.tools.logging :as log]))

(defn- meet-challenge? [conn-req wallet]
  (let [challenge-set (set (:pool-challenge conn-req))
        bag-key-set (-> wallet :bag keys set)]
    (log/debug "Challenge: " (cs/join ", " challenge-set))
    (log/debug "Bag keys: " (cs/join ", " bag-key-set))
    (if (set/subset? challenge-set bag-key-set)
      conn-req
      (ed/precondition-failed (str "Missing from bag: "
                                   (cs/join ", " (set/difference challenge-set
                                                                 bag-key-set)))))))

(fact "unable to meet the challenge"
  (let [conn-req {:pool-challenge ["dob"]}
        wallet {:bag {"name" "a-name"}}
        challenged (meet-challenge? conn-req wallet)]
    (:status challenged) => 412
    (:message challenged) => "Missing from bag: dob"))

(defn- add-trust-pool [conn-req wallet]
  (if-let [pool (ag/w->tp wallet (:pool-identity conn-req))]
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
                   :pool-challenge ["challenge"]} ..wallet..) => ..linked-pool..
  (provided
    (ag/w->tp ..wallet.. "pool id") => nil
    (tpr/save anything) => "trust pool"
    (ag/trustpool "trust pool" ..wallet..) => ..linked-pool..))

(defn accept [conn-req wallet updates]
  (fail-> (meet-challenge? conn-req wallet)
          (add-trust-pool wallet)))
