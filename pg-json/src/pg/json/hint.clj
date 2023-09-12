(ns pg.json.hint
  (:import
   clojure.lang.IPersistentMap)
  (:require
   [pg.oid :as oid]
   [pg.types.hint :as hint]))


(hint/add-hint IPersistentMap oid/json)
