(ns pg.json.decode.txt
  (:refer-clojure :exclude [extend])
  (:require
   [cheshire.core :as json]
   [pg.oid :as oid]
   [pg.decode.txt :refer [extend]]))


(extend [oid/json oid/jsonb]
  [string _ _]
  (json/parse-string string keyword))
