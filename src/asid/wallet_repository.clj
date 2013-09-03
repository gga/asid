(ns asid.wallet-repository
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

(defn attach-to-node [node link data]
  (let [data-node (nn/create data)]
        (nrl/create node data-node link)))

(defn save [ctxt wallet]
  (let [node (nn/create {:identity (:identity wallet)})]
    (doseq [data [:bag :signatures :key]]
      (attach-to-node node data (get wallet data)))
    (nrl/create (:root ctxt) node :wallet)
    wallet))
