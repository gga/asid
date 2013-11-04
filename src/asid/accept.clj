(ns asid.accept
  (:use midje.sweet
        [asid.error.thread :only [fail->]] )

  (:require [asid.error.definition :as ed]
            [asid.graph :as ag]
            [clojure.set :as set]
            [clojure.string :as cs]
            [clojure.tools.logging :as log]))

(defn- meet-challenge? [conn-req wallet]
  (let [challenge-set (set (:pool-challenge conn-req))]
    (log/debug "Challenge: " (cs/join ", " challenge-set))
    (log/debug "Bag: " (cs/join ", " (-> wallet :bag keys set)))
    (if (set/subset? challenge-set (-> wallet :bag keys set))
     conn-req
     (ed/precondition-failed (str "Must have in bag: " (cs/join ", " challenge-set))))))

(fact "unable to meet the challenge"
  (let [conn-req {:pool-challenge ["dob"]}
        wallet {:bag {"name" "a-name"}}
        challenged (meet-challenge? conn-req wallet)]
    (:status challenged) => 412))

(defn accept [conn-req wallet updates]
  (fail-> (meet-challenge? conn-req wallet)))

(fact
  (accept ..conn-req.. ..wallet.. ..updates..) => ..conn-req..
  (provided
    (meet-challenge? ..conn-req.. ..wallet..) => ..conn-req..))

