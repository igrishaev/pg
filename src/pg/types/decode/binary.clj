(ns pg.types.decode.binary
  "
  https://hackage.haskell.org/package/postgresql-binary-0.9/docs/src/PostgreSQL-Binary-Decoder.html
  "
  (:import
   java.util.UUID)
  (:require
   [pg.oid :as oid]
   [pg.codec :as codec]
   [pg.bb :as bb]))


(defmulti decode
  (fn [buf field enc]
    (:type-id field)))


(defmethod decode :default
  [buf field enc]
  buf)


(defmethod decode oid/FLOAT4
  [^bytes buf field enc]
  (Float/intBitsToFloat (new BigInteger buf)))


(defmethod decode oid/TEXT
  [^bytes buf field enc]
  (codec/bytes->str buf enc))


(defmethod decode oid/VARCHAR
  [^bytes buf field enc]
  (codec/bytes->str buf enc))


(defmethod decode oid/BPCHAR
  [^bytes buf field enc]
  (codec/bytes->str buf enc))


(defmethod decode oid/NAME
  [^bytes buf field enc]
  (codec/bytes->str buf enc))


(defmethod decode oid/CHAR
  [^bytes buf field enc]
  (-> buf (aget 0) char))


(defmethod decode oid/FLOAT8
  [^bytes buf field enc]
  (Double/longBitsToDouble (new BigInteger buf)))


(defmethod decode oid/INT2
  [^bytes buf field enc]
  (.shortValue (new BigInteger buf)))


(defmethod decode oid/UUID
  [^bytes buf field enc]
  (let [bb (bb/wrap buf)
        l1 (bb/read-long8 bb)
        l2 (bb/read-long8 bb)]
    (new UUID l1 l2)))


(defmethod decode oid/OID
  [^bytes buf field enc]
  (.intValue (new BigInteger buf)))


(defmethod decode oid/INT4
  [^bytes buf field enc]
  (.intValue (new BigInteger buf)))


(defmethod decode oid/INT8
  [^bytes buf field enc]
  (.longValue (new BigInteger buf)))


(defmethod decode oid/MONEY
  [^bytes buf field enc]
  (new BigInteger buf))


(defmethod decode oid/BYTEA
  [^bytes buf field enc]
  buf)


(defmethod decode oid/VOID
  [^bytes buf field enc])


#_
(defmethod decode oid/NUMERIC
  [^bytes buf field enc]
  )


(defmethod decode oid/BOOL
  [^bytes buf field enc]
  (case (aget buf 0)
    0 false
    1 true))
