(ns pg.json.encode.bin
  (:require
   [cheshire.core :as json]
   [pg.encode.bin :refer [expand]]
   [pg.oid :as oid]))


(defn get-client-encoding ^String [opt]
  (get opt :client-encoding "UTF-8"))


(expand [Object oid/json
         Object oid/jsonb]
  [obj _ opt]

  (let [encoding
        (get-client-encoding opt)]

    (-> obj
        (json/generate-string obj)
        (.getBytes encoding))))


(expand [String oid/json
         String oid/jsonb]
  [^String string _ opt]

  (let [encoding
        (get-client-encoding opt)]

    (.getBytes string encoding)))
