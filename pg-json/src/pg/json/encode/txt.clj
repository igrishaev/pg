(ns pg.json.encode.txt
  (:import
   java.util.Map)
  (:require
   [cheshire.core :as json]
   [pg.encode.txt :as txt]
   [pg.oid :as oid]))


(defmethod txt/-encode [Object oid/json]
  [obj _ _]
  (json/generate-string obj))


(defmethod txt/-encode [Object oid/jsonb]
  [obj _ _]
  (json/generate-string obj))


(defmethod txt/-encode [String oid/json]
  [obj _ _]
  obj)


(defmethod txt/-encode [String oid/jsonb]
  [obj _ _]
  obj)


(txt/set-default Map oid/json)
