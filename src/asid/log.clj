(ns asid.log
  (:use ring.util.request)

  (:require [clojure.tools.logging :as log]))

(defn inbound-request [handler]
  (fn [req]
    (log/info "Method: " (:request-method req) " uri: " (request-url req))
    (log/debug "Headers: " (:headers req))
    (handler req)))

(defn outbound-response [handler]
  (fn [req]
    (try
      (let [resp (handler req)]
        (log/info "Status: " (:status resp))
        (log/debug "Headers: " (:headers resp))
        (log/debug "Body: " (:body resp))
        resp)
      (catch Exception ex
        (log/error ex "Request failed for uri: " (:uri req))))))
