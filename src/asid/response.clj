(ns asid.response
  (:use midje.sweet)
  (:require [ring.util.response :as rr]
            [asid.render :as render]))

(defn resource [obj]
  (let [json-body (render/to-json obj)]
    (-> json-body
        rr/response
        (rr/content-type (render/content-type obj))
        (rr/header "Location" (-> json-body :links :self)))))

(defn created [body]
  (-> body
      resource
      (rr/status 201)))

(fact
  (created {:body true}) => (contains {:body {:body true}})
  (created {:body true}) => (contains {:status 201})
  (created {:body true}) => (contains {:headers (contains {"Content-Type" "application/vnd.clojure.map+json"})})
  (created {:links {:self "uri"}}) => (contains {:headers (contains {"Location" "uri"})}))
