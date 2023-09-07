(ns pg.decode.bin
  (:import
   java.math.BigDecimal
   java.util.UUID)
  (:require
   [clojure.template :refer [do-template]]
   [pg.bytes :as bytes]
   [pg.bb :as bb]
   [pg.oid :as oid]
   [pg.decode.bin.datetime :as datetime]
   [pg.decode.bin.numeric :as numeric]))


(defn get-server-encoding ^String [opt]
  (get opt :server-encoding "UTF-8"))


(defmulti -decode
  (fn [^bytes _buf oid _opt]
    oid))


(defmethod -decode :default
  [buf _ _]
  buf)


(defmacro expand
  {:style/indent 1}
  [oid's binding & body]
  `(do-template [oid#]
                (defmethod -decode oid#
                  ~binding
                  ~@body)
                ~@oid's))


;;
;; API
;;

(defn decode

  ([^bytes buf oid]
   (-decode buf oid nil))

  ([^bytes buf oid opt]
   (-decode buf oid opt)))


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
;; BigDecimal
;;


(expand [oid/numeric]
  [buf _ opt]
  (numeric/BigDecimal-numeric buf opt))

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
;; Byte array
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


;;
;; Date & time
;;

(expand [oid/time]
  [^bytes buf _ opt]
  (datetime/parse-time buf opt))


(expand [oid/timetz]
  [^bytes buf _ opt]
  (datetime/parse-timetz buf opt))


(expand [oid/timestamp]
  [^bytes buf _ opt]
  (datetime/parse-timestamp buf opt))


(expand [oid/timestamptz]
  [^bytes buf _ opt]
  (datetime/parse-timestamptz buf opt))


(expand [oid/date]
  [^bytes buf _ opt]
  (datetime/parse-date buf opt))


;;
;; Arrays
;;

#_
(expand [oid/_bool
         oid/_int2
         oid/_int4
         oid/_int8]
  [^bytes buf array-oid opt]
  (let [oid (array-oid->oid array-oid)
        method (get-method -decode oid)]
    (array/decode buf method opt)))
