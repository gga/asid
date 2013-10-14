(ns asid.file-resource
  (:use ring.middleware.resource
        ring.util.response)

  (:import [java.io File]))

(defn resources [handler]
  (wrap-resource handler "public"))

(defn file-resource [path]
  (let [res-dir (File. "resources")]
    (if (.exists res-dir)
      (File. (str "resources/public/" path))
      (resource-response path {:root "public"}))))
