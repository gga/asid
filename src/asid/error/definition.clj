(ns asid.error.definition
  (:use midje.sweet))

(defrecord Failure [status message])

(defn bad-request-data [msg]
  (Failure. 400 msg))

(defn- check-keyword [data check param]
  check)

(defmulti validate! check-keyword)

(defmethod validate! :not-empty [data _ name]
  (if (empty? (get data name))
    (bad-request-data (str name " is required."))
    data))
