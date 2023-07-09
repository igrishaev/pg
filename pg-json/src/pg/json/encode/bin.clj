(ns pg.json.encode.bin
  (:refer-clojure :exclude [extend])
  (:require
   [cheshire.core :as json]
   [pg.encode.bin :refer [extend]]
   [pg.oid :as oid]))


(defn get-client-encoding ^String [opt]
  (get opt :client-encoding "UTF-8"))


(extend [Object oid/json
         Object oid/jsonb]
  [obj _ opt]

  (let [encoding
        (get-client-encoding opt)]

    (-> obj
        (json/generate-string obj)
        (.getBytes encoding))))


(extend [String oid/json
         String oid/jsonb]
  [^String string _ opt]

  (let [encoding
        (get-client-encoding opt)]

    (.getBytes string encoding)))
