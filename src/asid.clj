(ns asid
  (:use ring.middleware.file-info)

  (:require [asid.routing :as rt]
            [asid.nodes :as an]
            [asid.log :as log] 
            [asid.content-negotiation :as acn]
            [asid.json-doc-exchange :as jde]
            [asid.static :as as]
            [asid.current-request :as req]
            [asid.file-resource :as afr]))

(defn- identity-handler [handler]
  (fn [req] (handler req)))

(defn app
  ([]
     (app identity-handler
          (an/initialize!)))

  ([http repo]
     {:web (-> (rt/uri-map repo)
               log/inbound-request
               req/capture-request
               jde/json-documents
               acn/accepts
               acn/vary-by-accept
               afr/resources
               wrap-file-info
               as/static-dir-index
               http
               log/outbound-response)
      :repo repo}))

