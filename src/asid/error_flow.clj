(ns asid.error-flow
  (:use midje.sweet)

  (:require [clojure.algo.monads :as monad]))

(defrecord Failure [status message])

(defprotocol Failed
  (has-failed? [failure]))

(extend-protocol Failed
  Object
  (has-failed? [failure] false)

  Failure
  (has-failed? [failure] true)

  Exception
  (has-failed? [failure] true))

(monad/defmonad failure-m
  [m-result identity
   m-bind (fn [m f]
            (if (has-failed? m)
              m
              (f m)))])

(defn add-monadic-expr [insert form]
  (list 'm-bind insert (if (seq? form)
                         `(fn [bound#] (~(first form) bound# ~@(rest form)))
                         `(fn [bound#] (~form bound#)))))

(defmacro f->
  ([x]
     `(monad/with-monad failure-m ~x))
  ([x form]
     `(monad/with-monad failure-m
        ~(add-monadic-expr x form)))
  ([x form & more]
     `(f-> (f-> ~x ~form) ~@more)))

(defn- succeed [v]
  v)
(defn- include [m k v]
  (conj m [k v]))
(defn- show [m k]
  (get m k))
(defn- fail [m limit]
  (if (> limit (:limit m))
    (Failure. 400 "Imposed limit exceeded")
    m))

(fact
  (f-> 10 (+ 9)) => 19
  (f-> {} succeed) => {}
  (f-> 10 (+ 9) (- 11)) => 8)

(defn- short-steps [limit]
  (f-> {:limit 50}
       (fail limit)))

(fact
  (short-steps 10) => {:limit 50}
  (short-steps 100) => (partial instance? Failure))

(defn- more-steps [limit]
  (f-> {}
       succeed
       (include :limit 50)
       (fail limit)
       (include :value 100)
       (show :value)))

(fact
  (more-steps 10) => 100
  (more-steps 100) => (partial instance? Failure))
