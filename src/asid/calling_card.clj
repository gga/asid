(ns asid.calling-card
  (:use midje.sweet)

  (:require [clojure.data.json :as json]
            [asid.identity :as aid]
            [asid.neo :as an]
            [asid.strings :as as]
            [asid.render :as render]
            [clj-http.client :as http]))

(defrecord CallingCard [identity id-uri other-party])

(defn new-calling-card [id-uri other-party-identity]
  (CallingCard. (aid/new-identity other-party-identity)
                id-uri
                other-party-identity))

;; Error handling!
(defn submit [card wallet pool]
  (let [other-identity (http/get (:id-uri card))
        id-doc (json/read-str (:body other-identity)
                              :key-fn keyword)]
    (http/post (as/resolve-url (-> :links :letterplate id-doc) (:id-uri card))
               {:body (json/write-str (render/to-json card))})))

(defn attach [card pool]
  (an/connect-nodes card pool :adds-identity)
  card)

(defn uri [card]
  (str "/card" (:identity card)))

(defn self-link [so-far card]
  (conj so-far [:self (uri card)]))

(defn links [card]
  (-> {}
      (self-link card)))

(extend-type CallingCard
  render/Resource

  (to-json [card]
    {:identity (:identity card)
     :otherParty (:other-party card)
     :links (links card)})

  (content-type [_]
    "application/vnd.org.asidentity.calling-card+json"))
