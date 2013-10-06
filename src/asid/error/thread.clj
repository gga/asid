(ns asid.error.thread
  (:use midje.sweet
        asid.error.definition)

  (:require [clojure.algo.monads :as monad])

  (:import [asid.error.definition Failure]))

(defn- bind-monadic-expr-into-form [insert form]
  (list 'm-bind insert (if (seq? form)
                         `(fn [bound#] (~(first form) bound# ~@(rest form)))
                         `(fn [bound#] (~form bound#)))))

(defmacro m->
  ([m x]
     `(monad/with-monad ~m ~x))
  ([m x form]
     `(monad/with-monad ~m
        ~(bind-monadic-expr-into-form x form)))
  ([m x form & more]
     `(m-> ~m (m-> ~m ~x ~form) ~@more)))

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

(defmacro fail->
  ([x] `(m-> ~failure-m ~x))
  ([x form] `(m-> failure-m ~x ~form))
  ([x form & more] `(fail-> (fail-> ~x ~form) ~@more)))

(defmacro dofailure [bindings expr]
  `(monad/domonad failure-m ~bindings ~expr))

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
  (fail-> 10 (+ 9)) => 19
  (fail-> {} succeed) => {}
  (fail-> 10 (+ 9) (- 11)) => 8)

(defn- short-steps [limit]
  (fail-> {:limit 50}
          (fail limit)))

(fact
  (short-steps 10) => {:limit 50}
  (short-steps 100) => (partial instance? Failure))

(defn- more-steps [limit]
  (fail-> {}
          succeed
          (include :limit 50)
          (fail limit)
          (include :value 100)
          (show :value)))

(fact
  (more-steps 10) => 100
  (more-steps 100) => (partial instance? Failure))
