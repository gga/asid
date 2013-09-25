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

(defn- nodes-by-direction [start type dir which-end]
  (remove nil? (map #(if-let [end-node (-> % which-end)]
                       (nn/fetch-from end-node)
                       nil)
                    (nrl/traverse (:id start)
                                  :relationships [{:direction dir :type type}]))))

(defn sub-nodes [start type]
  (nodes-by-direction start type "out" :end))

(defn parent-nodes [start type]
  (nodes-by-direction start type "in" :start))

(defn sub-node [start type]
  (first (sub-nodes start type)))

(defn sub-objects [start type]
  (if-let [node-id (:node-id start)]
    (map :data (sub-nodes {:id node-id} type))
    []))

(defn parent-objects [start type]
  (if-let [node-id (:node-id start)]
    (map :data (parent-nodes {:id node-id} type))
    []))

(defn parent-object [start type]
  (first (parent-objects start type)))
