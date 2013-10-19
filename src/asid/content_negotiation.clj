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

(defn by-content* [accepts content-map]
  (let [handler (first (remove nil? (map content-map accepts)))]
    (handler)))

(defn- build-content-handlers [types-fns]
  (reduce #(conj %1 %2)
          (map (fn [[type func]] {type `(fn [] ~func)})
               (partition 2 types-fns))))

(fact "about building single content handler maps"
  (let [handlers (build-content-handlers ["type" :function])
        type-handler (get handlers "type")]
    type-handler => `(fn [] :function)))

(fact "about building multiple content handler maps"
  (let [handlers (build-content-handlers ["text" :function
                                          "html" :function])
        html-handler (get handlers "html")
        text-handler (get handlers "text")]
    text-handler => `(fn [] :function)
    html-handler => `(fn [] :function)))

(defmacro by-content
  ([accepts & types]
     `(by-content* ~accepts ~(build-content-handlers types))))
