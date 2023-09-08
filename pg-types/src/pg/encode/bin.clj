(ns pg.encode.bin
  (:require
   [pg.encode.bin.core :refer [-encode]]
   [pg.encode.bin.basic]
   [pg.encode.bin.numeric]
   [pg.encode.bin.datetime]
   [pg.encode.bin.array]))


;;
;; API
;;

(defn encode
  (^bytes [value]
   (-encode value nil nil))

  (^bytes [value oid]
   (-encode value oid nil))

  (^bytes [value oid opt]
   (-encode value oid opt)))
