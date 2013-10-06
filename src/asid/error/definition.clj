(ns asid.error.definition
  (:use midje.sweet))

(defrecord Failure [status message])

(defn bad-request-data [msg]
  (Failure. 400 msg))

(defn- vararg-keyword [first & rest]
  (keyword first))

(defmulti validate! vararg-keyword)

(defmethod validate! :not-empty [_ param name]
  (if (empty? param)
    (bad-request-data (str name " is required."))
    param))
