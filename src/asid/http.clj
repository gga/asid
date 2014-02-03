(ns asid.http
  (:use midje.sweet
        [asid.error.thread :only [fail->]])

  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [asid.error.definition :as ed]))

(defn- http-wrapper [method uri options]
  (log/debug "Outbound URI: " uri)
  (log/debug "Outbound Options: " options)
  (let [response (method uri options)]
    (log/debug "Status: " (:status response))
    (log/debug "Headers: " (:headers response))
    (log/debug "Body: " (:body response))
    (fail-> response
            ed/http-failed?
            (conj [:body
                   (json/read-str (:body response) :key-fn keyword)]))))

(defmacro verb [name wrap]
  `(defn ~name [uri# options#]
     (http-wrapper ~wrap uri# options#)))

(verb get client/get)
(verb put client/put)
(verb post client/post)
(verb delete client/delete)
