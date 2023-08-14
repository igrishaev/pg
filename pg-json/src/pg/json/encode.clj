(ns pg.json.encode
  (:require
   [cheshire.core :as json]
   [pg.oid :as oid]
   [pg.encode.txt :as txt]
   [pg.encode.hint :as hint]
   [pg.encode.bin :as bin]))


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

(defn get-client-encoding ^String [opt]
  (get opt :client-encoding "UTF-8"))


(bin/expand [Object oid/json
             Object oid/jsonb]
  [obj _ opt]

  (let [encoding
        (get-client-encoding opt)]

    (-> obj
        (json/generate-string obj)
        (.getBytes encoding))))


(bin/expand [String oid/json
             String oid/jsonb]
  [^String string _ opt]

  (let [encoding
        (get-client-encoding opt)]

    (.getBytes string encoding)))


;;
;; Hint
;;

;; TODO: refactor this
(hint/add-hint clojure.lang.PersistentArrayMap oid/json)
