(ns pg.types.decode
  (:require
   [pg.error :as e]
   [pg.types.decode.binary :as bin]
   [pg.types.decode.text :as text]))


(defn decode [format oid buf opt]
  (case format
    0
    (bin/mm-decode oid buf opt)
    1
    (text/mm-decode oid buf opt)
    ;; else
    (e/error! "Wrong field format: neither 0 or 1"
              {:format format
               :oid oid
               :opt opt})))
