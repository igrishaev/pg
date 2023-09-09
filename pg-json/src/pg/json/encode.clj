(ns pg.json.encode
  (:require
   [cheshire.core :as json]
   [pg.oid :as oid]
   [pg.encode.txt.core :as txt]
   [pg.encode.bin.core :as bin]))


;;
;; Txt
;;

(txt/expand [Object oid/json
             Object oid/jsonb]
  [obj _ _]
  (json/generate-string obj))


(txt/expand [String oid/json
             String oid/jsonb]
  [string _ _]
  string)


;;
;; Bin
;;

(bin/expand [Object oid/json
             Object oid/jsonb]
  [obj _ opt]

  (let [encoding
        (bin/get-client-encoding opt)]

    (-> obj
        (json/generate-string obj)
        (.getBytes encoding))))


(bin/expand [String oid/json
             String oid/jsonb]
  [^String string _ opt]

  (let [encoding
        (bin/get-client-encoding opt)]

    (.getBytes string encoding)))
