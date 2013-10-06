(ns asid.error.definition
  (:use midje.sweet))

(defrecord Failure [status message])

(defn bad-request [msg]
  (Failure. 400 msg))

(defn not-found []
  (Failure. 404 "Not found."))

(defn- check-keyword [data check param]
  check)

(defmulti validate! check-keyword)

(defmethod validate! :not-empty [data _ name]
  (if (empty? (get data name))
    (bad-request (str name " is required."))
    data))
