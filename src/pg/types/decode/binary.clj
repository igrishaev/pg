(ns pg.types.decode.binary
  "
  https://hackage.haskell.org/package/postgresql-binary-0.9/docs/src/PostgreSQL-Binary-Decoder.html
  https://github.com/postgres/postgres/blob/master/src/backend/utils/adt/varbit.c
  "
  (:import
   java.util.UUID)
  (:require
   [pg.oid :as oid]
   [pg.codec :as codec]
   [pg.bb :as bb]))


(defmacro get-enc [options]
  `(get ~options :encoding "UTF-8"))


(defmulti mm-decode
  (fn [oid _ _]
    oid))


(defmethod mm-decode :default
  [_ ^bytes buf _]
  buf)


(defmethod mm-decode oid/FLOAT4
  [_ ^bytes buf _]
  (Float/intBitsToFloat (new BigInteger buf)))


(defmethod mm-decode oid/TEXT
  [_ ^bytes buf opt]
  (codec/bytes->str buf (get-enc opt)))


(defmethod mm-decode oid/VARCHAR
  [_ ^bytes buf opt]
  (codec/bytes->str buf (get-enc opt)))


(defmethod mm-decode oid/BPCHAR
  [_ ^bytes buf opt]
  (codec/bytes->str buf (get-enc opt)))


(defmethod mm-decode oid/NAME
  [_ ^bytes buf opt]
  (codec/bytes->str buf (get-enc opt)))


(defmethod mm-decode oid/CHAR
  [_ ^bytes buf _]
  (-> buf (aget 0) char))


(defmethod mm-decode oid/FLOAT8
  [_ ^bytes buf _]
  (Double/longBitsToDouble (new BigInteger buf)))


(defmethod mm-decode oid/INT2
  [_ ^bytes buf _]
  (.shortValue (new BigInteger buf)))


(defmethod mm-decode oid/UUID
  [_ ^bytes buf _]
  (let [bb (bb/wrap buf)
        l1 (bb/read-long8 bb)
        l2 (bb/read-long8 bb)]
    (new UUID l1 l2)))


(defmethod mm-decode oid/OID
  [_ ^bytes buf _]
  (.intValue (new BigInteger buf)))


(defmethod mm-decode oid/INT4
  [_ ^bytes buf _]
  (.intValue (new BigInteger buf)))


(defmethod mm-decode oid/INT8
  [_ ^bytes buf _]
  (.longValue (new BigInteger buf)))


(defmethod mm-decode oid/MONEY
  [_ ^bytes buf _]
  (new BigInteger buf))


(defmethod mm-decode oid/BYTEA
  [_ ^bytes buf _]
  buf)


(defmethod mm-decode oid/VOID
  [_ ^bytes buf _])


#_
(defmethod mm-decode oid/NUMERIC
  [_ ^bytes buf _]
  )


(defmethod mm-decode oid/BOOL
  [_ ^bytes buf _]
  (case (aget buf 0)
    0 false
    1 true))


(defmethod mm-decode oid/BIT
  [_ ^bytes buf _]
  (let [bb
        (bb/wrap buf)

        bits-count
        (bb/read-int32 bb)

        bytes-rest
        (bb/read-rest bb)

        ;; len-bits
        ;; (alength bits-rest)

        pad-count
        (- 8 (alength bytes-rest))

        bytes-full
        (byte-array
         (concat (repeat pad-count 0) bytes-rest))

        int-value
        (new BigInteger bytes-full)

        ]

    int-value
    )
)


(defn decode
  ([oid buf]
   (mm-decode oid buf nil))
  ([oid buf options]
   (mm-decode oid buf options)))
