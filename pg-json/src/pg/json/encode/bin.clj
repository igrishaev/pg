(ns pg.json.encode.bin
  (:refer-clojure :exclude [extend])
  (:require
   [cheshire.core :as json]
   [pg.encode.bin :refer [extend]]
   [pg.oid :as oid]))
