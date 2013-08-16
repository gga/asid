(defproject asid "0.0.1-SNAPSHOT"
  :description "Cool new project to do things and stuff"
  :dependencies [[org.clojure/clojure "1.5.0"]
                 [compojure "1.1.5"]
                 [enlive "1.1.1"]
                 [ring/ring-core "1.2.0"]
                 [ring/ring-jetty-adapter "1.2.0"]]
  :profiles {:dev {:dependencies [[midje "1.5.0"]]}}

  :ring {:handler asid/app})
  
