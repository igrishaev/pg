(ns pg.json.decode
  (:require
   [cheshire.core :as json]
   [pg.oid :as oid]
   [pg.decode.txt :as txt]
   [pg.decode.bin.core :as bin]))


;;
;; Txt
;;

(txt/expand [oid/json oid/jsonb]
  [string _ _]
  (json/parse-string string keyword))


;;
;; Bin
;;

(defn get-server-encoding ^String [opt]
  (get opt :server-encoding "UTF-8"))


(bin/expand [oid/json oid/jsonb]
  [^bytes buf _ opt]

  (let [encoding
        (get-server-encoding opt)

        string
        (new String buf encoding)]

    (json/parse-string string keyword)))
