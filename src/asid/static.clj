(ns asid.static
  (:use midje.sweet))

(defn static-dir-index [handler]
  (fn [req]
    (handler
     (update-in req [:uri] #(if (re-find #"/$" %)
                              (str % "index.html")
                              %)))))

(fact
  (let [wrapper (static-dir-index #(:uri %))]
    (wrapper {:uri "/"}) => "/index.html"
    (wrapper {:uri "/path/"}) => "/path/index.html"
    (wrapper {:uri "/path/sub/"}) => "/path/sub/index.html"
    (wrapper {:uri "/path"}) => "/path"))
