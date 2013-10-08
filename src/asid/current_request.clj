(ns asid.current-request
  (:use midje.sweet)

  (:require [asid.strings :as as]))

(def ^:dynamic *current-request* nil)

(defn capture-request [handler]
  (fn [req]
    (binding [*current-request* req]
      (handler req))))

(defn url-relative-to-request [rel-uri]
  (as/resolve-url rel-uri (:uri *current-request*)))

(fact
  (binding [*current-request* {:uri "http://localhost/"}]
    (url-relative-to-request "/hello") => "http://localhost/hello")
  (binding [*current-request* {:uri "http://google.com/search/query=hello"}]
    (url-relative-to-request "/world") => "http://google.com/world"))
