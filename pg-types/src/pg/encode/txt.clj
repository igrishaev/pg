(ns pg.encode.txt
  (:require
   [pg.encode.txt.core
    :refer [-encode]]
   [pg.encode.txt.basic]
   [pg.encode.txt.datetime]
   [pg.encode.txt.array]))


;;
;; API
;;

(defn encode
  (^String [value]
   (-encode value nil nil))

  (^String [value oid]
   (-encode value oid nil))

  (^String [value oid opt]
   (-encode value oid opt)))
