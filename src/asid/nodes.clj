(ns asid.nodes
  (:require [asid.strings :as as]
            [asid.error.definition :as ed]
            [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]
            [clojurewerkz.neocons.rest.cypher :as cy]))

(defn- build-context [root-node]
  {:root root-node
   :node-id (:id root-node)})

(defn initialize! []
  (let [neo-host (as/getenv "NEO4J_URL" "http://localhost:7474")]
    (nr/connect! (str neo-host "/db/data"))
    (let [root (nn/get 0)
          asid-rels (nrl/outgoing-for root :types [:asid])]
      (if (seq asid-rels)
        (build-context (-> asid-rels first :end nn/fetch-from))
        (let [asid-root (nn/create {:name "asid root"})]
          (nrl/create root asid-root :asid)
          (build-context asid-root))))))

(defn node-to-object [node]
  (conj (:data node) [:node-id (:id node)]))

(defn associate-node [data-map node]
  (conj data-map [:node-id (:node-id node)]))

(defn create-node [data-map]
  {:node-id (:id (nn/create data-map))})

(defn node-from [obj]
  (:node-id obj))

(defn clean-node [node]
  (dissoc node :node-id))

(defn has-node? [obj-or-map]
  (contains? obj-or-map :node-id))

(defn update-node [obj data]
  (nn/update (:node-id obj) data))

(defn connect-nodes [from to link]
  (nrl/create (:node-id from) (:node-id to) link))

(defn attach-to-node [obj link data]
  (let [data-node (nn/create data)]
        (nrl/create (:node-id obj) data-node link)))

(defn- objects-by-direction [start type dir which-end]
  (if (has-node? start)
    (let [found-nodes (remove nil?
                              (map #(if-let [end-node (-> % which-end)]
                                      (nn/fetch-from end-node)
                                      nil)
                                   (nrl/traverse (:node-id start)
                                                 :relationships [{:direction dir :type type}])))]
      (map node-to-object found-nodes))
    []))

(defn children [start type]
  (objects-by-direction start type "out" :end))

(defn superiors [start type]
  (objects-by-direction start type "in" :start))

(defn child [start type]
  (first (children start type)))

(defn superior [start type]
  (first (superiors start type)))

(defn node-with-identity [origin rel identity]
  (let [results (cy/tquery (str "START origin=node({originnode}) "
                                "MATCH origin-[" rel "]->dest "
                                "WHERE dest.identity = {destid} "
                                "RETURN dest")
                           {:originnode (:node-id origin)
                            :destid identity})]
    (if (not (empty? results))
      (-> results
          first
          (get "dest")
          :self
          nn/fetch-from
          node-to-object))))

(defn nodes-by-cypher [query params column]
  (let [results (cy/tquery query params)]
    (if (not (empty? results))
        (map #(-> % (get column) :self nn/fetch-from node-to-object) results))))
