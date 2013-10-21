(ns asid.nodes
  (:require [asid.strings :as as]
            [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]))

(defn initialize! []
  (let [neo-host (as/getenv "NEO4J_URL" "http://localhost:7474")]
    (nr/connect! (str neo-host "/db/data"))
    (let [root (nn/get 0)
          asid-rels (nrl/outgoing-for root :types [:asid])]
      (if (seq asid-rels)
        {:root (-> asid-rels first :end nn/fetch-from)}
        (let [asid-root (nn/create {:name "asid root"})]
          (nrl/create root asid-root :asid)
          {:root asid-root})))))

(defn create-node [data-map]
  (:id (nn/create data-map)))

(defn associate-node [data-map node]
  (conj data-map [:node-id node]))

(defn has-node? [obj-or-map]
  (:node-id obj-or-map))

(defn connect-nodes [from to link]
  (nrl/create (:node-id from) (:node-id to) link))

(defn attach-to-node [obj link data]
  (let [data-node (nn/create data)]
        (nrl/create (:node-id obj) data-node link)))

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
