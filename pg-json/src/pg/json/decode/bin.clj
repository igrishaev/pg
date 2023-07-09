(ns pg.json.decode.bin
  (:refer-clojure :exclude [extend])
  (:require
   [cheshire.core :as json]
   [pg.oid :as oid]
   [pg.decode.bin :refer [extend]]))


(defn get-server-encoding ^String [opt]
  (get opt :server-encoding "UTF-8"))


(extend [oid/json oid/jsonb]
  [^bytes buf _ opt]

  (let [encoding
        (get-server-encoding opt)

        string
        (new String buf encoding)]

    (json/parse-string string keyword)))
