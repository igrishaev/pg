(ns pg.decode.txt
  (:require
   [pg.decode.txt.core :refer [-decode]]
   [pg.decode.txt.basic]
   [pg.decode.txt.datetime]
   [pg.decode.txt.array]))


;;
;; API
;;

(defn decode

  ([string oid]
   (-decode string oid nil))

  ([string oid opt]
   (-decode string oid opt)))
