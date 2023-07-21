(ns pg.json.encode.txt
  (:require
   [cheshire.core :as json]
   [pg.encode.txt :refer [expand]]
   [pg.oid :as oid]))


(expand [Object oid/json
         Object oid/jsonb]
  [obj _ _]
  (json/generate-string obj))


(expand [String oid/json
         String oid/jsonb]
  [string _ _]
  string)
