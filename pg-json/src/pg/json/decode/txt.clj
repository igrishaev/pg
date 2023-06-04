(ns pg.json.decode.txt
  (:require
   [cheshire.core :as json]
   [pg.oid :as oid]
   [pg.decode.txt :as txt]))


(defmethod txt/-decode oid/json
  [string _ opt]
  (let [fn-key
        (-> opt :fn-json-key (or keyword))]
    (json/parse-string string fn-key)))


(defmethod txt/-decode oid/jsonb
  [string _ opt]
  (let [fn-key
        (-> opt :fn-json-key (or keyword))]
    (json/parse-string string fn-key)))
