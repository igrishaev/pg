(ns pg.decode.bin.basic
  (:require
   [pg.oid :as oid]
   [pg.bb :as bb]
   [pg.bytes :as bytes]
   [pg.decode.bin.core
    :refer [expand
            get-server-encoding]])
  (:import
   java.util.UUID))


;;
;; Numbers
;;

(expand [oid/int2]
  [buf _ _]
  (bytes/bytes->int16 buf))


(expand [oid/int4 oid/oid]
  [buf _ _]
  (bytes/bytes->int32 buf))


(expand [oid/int8]
  [buf _ _]
  (bytes/bytes->int64 buf))


(expand [oid/float4]
  [buf _ _]
  (bytes/bytes->float4 buf))


(expand [oid/float8]
  [buf _ _]
  (bytes/bytes->float8 buf))


;;
;; Text
;;

(expand [oid/text
         oid/varchar
         oid/name]
  [^bytes buf _ opt]
  (let [encoding
        (get-server-encoding opt)]
    (new String buf encoding)))


(expand [oid/bpchar]
  [^bytes buf _ opt]
  (let [encoding
        (get-server-encoding opt)]
    (first (new String buf encoding))))


;;
;; UUID
;;

(expand [oid/uuid]
  [^bytes buf _ opt]
  (let [bb (bb/wrap buf)
        l1 (bb/read-int64 bb)
        l2 (bb/read-int64 bb)]
    (new UUID l1 l2)))


;;
;; Bytes
;;

(expand [oid/bytea]
  [^bytes buf _ _]
  buf)


;;
;; Void
;;

(expand [oid/void]
  [_ _ _]
  (do nil))


;;
;; Bool
;;

(expand [oid/bool]
  [^bytes buf _ _]
  (case (aget buf 0)
    0 false
    1 true))
