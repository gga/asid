(ns asid.neo
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]))

(defn initialize! []
  (nr/connect! "http://localhost:7474/db/data/")
  (let [root (nn/get 0)
        asid-rels (nrl/outgoing-for root :types [:asid])]
    (if (seq asid-rels)
      {:root (-> asid-rels first :end nn/fetch-from)}
      (let [asid-root (nn/create {:name "asid root"})]
        (nrl/create root asid-root :asid)
        {:root asid-root}))))

(defn connect-nodes [from to link]
  (nrl/create (:node-id from) (:node-id to) link))

(defn attach-to-node [node link data]
  (let [data-node (nn/create data)]
        (nrl/create node data-node link)))

(defn sub-node [start type]
  (first (map #(-> % :end nn/fetch-from)
              (nrl/traverse (:id start)
                            :relationships [{:direction "out" :type type}]))))
