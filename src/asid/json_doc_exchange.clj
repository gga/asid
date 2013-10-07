(ns asid.json-doc-exchange
  (:use midje.sweet)

  (:require [clojure.data.json :as json]
            [ring.util.response :as rr]
            [ring.mock.request :as mr]))

(defn- ct [req-or-resp]
  (let [hdrs (:headers req-or-resp)]
    (cond
     (contains? hdrs "Content-Type") (get hdrs "Content-Type")
     (contains? hdrs "content-type") (get hdrs "content-type")
     :else "")))

(fact
  (ct (mr/request :get "/")) => ""
  (ct (-> (mr/request :get "/") (mr/header "Content-Type" "type"))) => "type"
  (ct (-> (rr/response "doc") (rr/content-type "type"))) => "type")

(defn- content-type-json? [req-or-resp]
  (re-find #"\+json\b" (ct req-or-resp)))

(fact
  (let [req (mr/request :get "/")]
    (content-type-json? req) => falsey
    (content-type-json? (-> req (mr/header "Content-Type" "app/vnd+json"))) => truthy
    (content-type-json? (-> req (mr/header "Content-Type" "app/vnd+json;charset=utf-8"))) => truthy)
  (let [resp (-> (rr/response "doc")
                 (rr/content-type "app+json"))]
    (content-type-json? resp) => truthy))

(defn json-documents [handler]
  (fn [req]
    (let [json-doc (if (content-type-json? req)
                     (try
                       (json/read-str (slurp (:body req)))
                       (catch Exception _ 
                         (-> (rr/response "JSON could not be parsed.")
                             (rr/status 400))))
                     nil)
          response (handler (conj req [:json-doc json-doc]))]
      (if (content-type-json? response)
        (conj response [:body (json/write-str (:body response))])
        response))))

(fact
  (let [wrapper (json-documents #(:json-doc %))]
    (wrapper (mr/request :post "/" "body")) => nil?
    (wrapper (-> (mr/request :post "/" "{\"key\":10}")
                 (mr/header "Content-Type" "app/test+json"))) => {"key" 10}
    (wrapper (-> (mr/request :post "/" "{\"malformed\": 10")
                 (mr/header "Content-Type" "app/test+json"))) => (contains {:status 400}))
  (let [wrapper (json-documents (fn [req] req))]
    (wrapper (-> (rr/response "hello"))) => (contains {:status 200
                                                       :body "hello"})
    (wrapper (-> (rr/response {:key 10})
                 (rr/content-type "app+json")))) => (contains {:status 200
                                                               :body "{\"key\":10}"}))
