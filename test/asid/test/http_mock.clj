(ns asid.test.http-mock
  (:use midje.sweet
        ring.util.response)

  (:require [compojure.core :as cc]
            [compojure.handler :as ch]
            [compojure.route :as route]
            [ring.mock.request :as mr]
            [robert.hooke :as hooke]
            [clj-http.client :as http]))

(defn make-server [base-uri routes]
  {:base-uri base-uri
   :routes (ch/site routes)})

(def ^:dynamic *mocked-server*)

(defn- rebuild-clj-response [{headers :headers :as resp}]
  (conj resp [:headers (into {} (for [[hdr val] headers] [(.toLowerCase hdr) val]))]))

(defn- mock-http-request [f request]
  (if (and (not (nil? *mocked-server*))
           (.startsWith (:url request) (:base-uri *mocked-server*)))
    (let [fake-handler (:routes *mocked-server*)
          ring-req (mr/request (:method request) (:url request))
          ring-req (conj ring-req [:headers (:headers request)])]
      (rebuild-clj-response (fake-handler ring-req)))
    (f request)))

(defmacro mock [base-uri & routes]
  `(make-server ~base-uri
                (cc/routes ~@routes (route/not-found (str "Mock handler not installed.")))))

(defmacro with-mock-http-server [fake-server & forms]
  `(binding [*mocked-server* ~fake-server] ~@forms))

(hooke/add-hook #'http/request #'mock-http-request)

(fact
  (let [fake-server (mock "http://example.com"
                          (cc/GET "/" []
                                  (-> (response (str "Caught http get."))
                                      (status 202))))]
    (with-mock-http-server fake-server
      (let [resp (http/get "http://example.com/")]
        (:status resp) => 202
        (:body resp) => "Caught http get."))))
