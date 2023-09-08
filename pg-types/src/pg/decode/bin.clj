(ns pg.decode.bin
  (:require
   [pg.decode.bin.core :as core]
   [pg.decode.bin.basic]
   [pg.decode.bin.numeric]
   [pg.decode.bin.datetime]
   [pg.decode.bin.array]))


;;
;; API
;;

(defn decode

  ([^bytes buf oid]
   (core/-decode buf oid nil))

  ([^bytes buf oid opt]
   (core/-decode buf oid opt)))
