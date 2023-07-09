(ns pg.json.encode.txt
  (:refer-clojure :exclude [extend])
  (:require
   [cheshire.core :as json]
   [pg.encode.txt :refer [extend]]
   [pg.oid :as oid]))


(extend [Object oid/json
         Object oid/jsonb]
  [obj _ _]
  (json/generate-string obj))


(extend [String oid/json
         String oid/jsonb]
  [string _ _]
  string)
