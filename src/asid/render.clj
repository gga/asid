(ns asid.render)

(defprotocol Linked
  (links [this] "Returns a map of generated links for the object."))

(defprotocol Resource
  (to-json [this] "JSON representation of the resource")
  (content-type [this] "Content type string for the resource"))
