(defproject asid "0.0.1-SNAPSHOT"
  :description "Cool new project to do things and stuff"
  :dependencies [[org.clojure/clojure "1.5.0"]
                 [org.clojure/data.codec "0.1.0"]
                 [org.clojure/data.json "0.2.2"]
                 [compojure "1.1.5"]
                 [enlive "1.1.4"]
                 [ring/ring-core "1.2.0"]
                 [ring/ring-jetty-adapter "1.2.0"]
                 [bouncycastle/bcprov-jdk16 "140"]
                 [clojurewerkz/neocons "1.1.0"]
                 [clj-http "0.7.7"]
                 [org.clojure/algo.monads "0.1.4"]]
  :profiles {:dev {:dependencies [[midje "1.5.0"]
                                  [lein-midje "3.0.0"]
                                  [ring-mock "0.1.5"]]}}

  :ring {:handler asid/app})
  
