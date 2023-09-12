(ns pg.types.array
  (:require
   [pg.types.hint :as hint]))


(defn guess-oid [matrix]
  (->> matrix
       (flatten)
       (filter some?)
       (first)
       (hint/hint)))
