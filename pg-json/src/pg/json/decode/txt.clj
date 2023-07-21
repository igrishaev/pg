(ns pg.json.decode.txt
  (:require
   [cheshire.core :as json]
   [pg.oid :as oid]
   [pg.decode.txt :refer [expand]]))


(expand [oid/json oid/jsonb]
  [string _ _]
  (json/parse-string string keyword))
