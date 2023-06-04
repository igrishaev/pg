(ns pg.decode.bin)


(defmulti -decode
  (fn [^bytes _buf oid _opt]
    oid))


;;
;; API
;;

(defn decode

  ([^bytes buf oid]
   (-decode buf oid nil))

  ([^bytes buf oid opt]
   (-decode buf oid opt)))
