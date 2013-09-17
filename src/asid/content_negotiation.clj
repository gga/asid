(ns asid.content-negotiation
  (:use ring.util.response
        midje.sweet)

  (:require [clojure.string :as cs]
            [ring.mock.request :as mr]))

(defn accepts [handler]
  (fn [req]
    (handler (assoc req :accepts
                    (map cs/trim (cs/split (-> req :headers (get "accept" "*/*"))
                                           #","))))))

(fact
  (let [wrapper (accepts #(:accepts %))
        req (mr/request :get "body")]
    (wrapper (mr/header req "Accept" "text/html")) => ["text/html"]
    (wrapper (mr/header req "Accept" "text/html, */*")) => ["text/html" "*/*"]))

(defn vary-by-accept [handler]
  #(header (handler %) "Vary" "Accept"))
