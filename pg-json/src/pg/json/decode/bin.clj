(ns pg.json.decode.bin
  (:require
   [cheshire.core :as json]
   [pg.oid :as oid]
   [pg.decode.bin :refer [expand]]))


(defn get-server-encoding ^String [opt]
  (get opt :server-encoding "UTF-8"))


(expand [oid/json oid/jsonb]
  [^bytes buf _ opt]

  (let [encoding
        (get-server-encoding opt)

        string
        (new String buf encoding)]

    (json/parse-string string keyword)))
