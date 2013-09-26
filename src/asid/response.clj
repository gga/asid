(ns asid.response
  (:use midje.sweet)
  (:require [ring.util.response :as rr]
            [asid.render :as render]))

(defn created [body ct]
  (let [json-body (render/to-json body)]
    (-> json-body
        rr/response
        (rr/content-type ct)
        (rr/header "Location" (-> json-body :links :self))
        (rr/status 201))))

(fact
  (created {:body true} "ct") => (contains {:body {:body true}})
  (created {:body true} "ct") => (contains {:status 201})
  (created {:body true} "ct") => (contains {:headers (contains {"Content-Type" "ct"})})
  (created {:links {:self "uri"}} "ct") => (contains {:headers (contains {"Location" "uri"})}))
