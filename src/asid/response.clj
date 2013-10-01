(ns asid.response
  (:use midje.sweet)
  (:require [ring.util.response :as rr]
            [asid.render :as render]))

(defn resource [obj]
  (let [links (render/links obj)
        json-body (render/to-json obj)]
    (-> (conj json-body [:links links])
        rr/response
        (rr/content-type (render/content-type obj))
        (rr/header "Location" (:self links)))))

(defn created [body]
  (-> body
      resource
      (rr/status 201)))


(extend-type clojure.lang.PersistentArrayMap
  render/Linked
  (links [m]
    {:self "map-link"}))

(extend-type clojure.lang.PersistentArrayMap
  render/Resource
  (to-json [m]
    m)

  (content-type [m]
    "application/vnd.clojure.map+json"))

(fact
  (render/to-json {:k "v"}) => {:k "v"})

(fact
  (created {:body true}) => (contains {:body (contains {:body true})})
  (created {:body true}) => (contains {:status 201})
  (created {:body true}) => (contains {:headers (contains {"Content-Type" "application/vnd.clojure.map+json"})})
  (created {}) => (contains {:headers (contains {"Location" "map-link"})}))
