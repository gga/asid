(ns asid.test.accept-facts
  (:use midje.sweet
        [asid.accept :only [accept]])

  (:require [asid.graph :as ag]
            [asid.trust-pool :as tp]
            [asid.trust-pool-repository :as tpr]
            [asid.test.http-mock :as hm]
            [asid.wallet :as w]))

(fact
  (let [conn-req {:pool-name "pool name"
                  :pool-identity "pool id"
                  :pool-challenge ["challenge"]}
        wallet (w/add-data (w/new-wallet "seed") "challenge" "value")]
    (accept conn-req wallet ..updates..) => ..trust-pool..
    (provided
      (ag/w->tp wallet "pool id") => nil
      (tp/new-trust-pool "pool id" "pool name" ["challenge"]) => ..trust-pool..
      (tpr/save ..trust-pool..) => ..trust-pool..
      (ag/trustpool ..trust-pool.. wallet) => ..trust-pool..)))


    
