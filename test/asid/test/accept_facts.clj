(ns asid.test.accept-facts
  (:use midje.sweet
        [asid.accept :only [accept]]
        compojure.core)

  (:require [asid.graph :as ag]
            [asid.trust-pool :as tp]
            [asid.trust-pool-repository :as tpr]
            [asid.test.http-mock :as hm]
            [asid.wallet :as w]
            [clojure.data.json :as json]))

(fact
  (let [conn-req {:pool-name "pool name"
                  :pool-identity "pool id"
                  :pool-challenge [:challenge]
                  :calling-card-uri "http://example.com/calling-card"}
        wallet (w/add-data (w/new-wallet "seed") :challenge "value")]
    (hm/with-mock-http-server
      (hm/mock "http://example.com"
               (GET "/calling-card" []
                    (json/write-str {:links {:challenge "http://example.com/challenge"}}))
               (PUT "/challenge" []
                    (json/write-str {:signatures {:identity "signed-trustee-id"
                                                  :challenge "signed-value-value"}})))

      (accept conn-req wallet ..updates..) => {:signatures {:identity "signed-trustee-id"
                                                            :challenge "signed-value-value"}}

      (provided
        (ag/w->tp wallet "pool id") => nil
        (tpr/save anything) => {:challenge ["challenge"]}
        (ag/trustpool {:challenge ["challenge"]} wallet) => {:challenge ["challenge"]}))))


    
