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
   [pg.bb :as bb]
   [pg.bytes :as b]))


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

        pad-count
        (- 8 (alength bytes-rest))

        bytes-full
        (if (zero? pad-count)
          bytes-rest
          (b/concat (byte-array pad-count) bytes-rest))

        shift-neg
        (mod bits-count 8)

        shift-pos
        (if (zero? shift-neg)
          0
          (- 8 shift-neg))

        int-val
        (-> (new BigInteger bytes-full)
            (.shiftRight shift-pos))]

    (loop [n 0
           mask (BigInteger/valueOf 1)
           acc ()]
      (println n mask (.and int-val mask))
      (if (= n bits-count)
        (vec acc)
        (let [bool
              (not (zero? (.and int-val mask)))]
          (recur (inc n)
                 (.shiftLeft mask 1)
                 (conj acc bool)))))))


(defn new-matrix
  [[dim & dimensions]]
  (if (and dim (seq dimensions))
    (vec (repeat dim (new-matrix dimensions)))
    []))


(defn matrix-path [dims n]
  (let [len (count dims)]
    (loop [i 0
           acc! (transient [])]
      (if (= i len)
        (persistent! acc!)
        (let [sub
              (subvec dims (inc i))
              idx
              (mod (quot n (reduce * sub)) (get dims i))]
          (recur (inc i) (conj! acc! idx)))))))


(defn decode-array
  ([buf]
   (decode-array buf nil))

  ([buf opt]
   (let [bb
         (bb/wrap buf)

         levels
         (bb/read-int32 bb)

         _
         (bb/read-int32 bb)

         oid
         (bb/read-int32 bb)

         dims
         (loop [i 0
                acc []]
           (if (= i levels)
             acc
             (let [len
                   (bb/read-int32 bb)
                   _
                   (bb/read-int32 bb)]
               (recur (inc i)
                      (conj acc len)))))

         total
         (reduce * dims)]

     (loop [i 0
            matrix (new-matrix dims)]

       (if (= i total)
         matrix
         (let [len
               (bb/read-int32 bb)

               item
               (when-not (= len -1)
                 (mm-decode oid (bb/read-bytes bb len) nil))

               path
               (matrix-path dims i)]

           (recur (inc i)
                  (update-in matrix (butlast path) conj item))))))))


(defmethod mm-decode oid/INT2_ARRAY
  [_ ^bytes buf opt]
  (decode-array buf opt))


(defmethod mm-decode oid/INT4_ARRAY
  [_ ^bytes buf opt]
  (decode-array buf opt))


(defmethod mm-decode oid/INT4_ARRAY
  [_ ^bytes buf opt]
  (decode-array buf opt))


(defmethod mm-decode oid/TEXT_ARRAY
  [_ ^bytes buf opt]
  (decode-array buf opt))


(defmethod mm-decode oid/FLOAT4_ARRAY
  [_ ^bytes buf opt]
  (decode-array buf opt))


(defmethod mm-decode oid/FLOAT8_ARRAY
  [_ ^bytes buf opt]
  (decode-array buf opt))


(defmethod mm-decode oid/BOOL_ARRAY
  [_ ^bytes buf opt]
  (decode-array buf opt))


(defmethod mm-decode oid/UUID_ARRAY
  [_ ^bytes buf opt]
  (decode-array buf opt))


(defmethod mm-decode oid/NUMERIC_ARRAY
  [_ ^bytes buf opt]
  (decode-array buf opt))


(defmethod mm-decode oid/BYTEA_ARRAY
  [_ ^bytes buf opt]
  (decode-array buf opt))


(defmethod mm-decode oid/OID_ARRAY
  [_ ^bytes buf opt]
  (decode-array buf opt))


(defmethod mm-decode oid/VARCHAR_ARRAY
  [_ ^bytes buf opt]
  (decode-array buf opt))


(defmethod mm-decode oid/BPCHAR_ARRAY
  [_ ^bytes buf opt]
  (decode-array buf opt))


(defmethod mm-decode oid/MONEY_ARRAY
  [_ ^bytes buf opt]
  (decode-array buf opt))


(defmethod mm-decode oid/BIT_ARRAY
  [_ ^bytes buf opt]
  (decode-array buf opt))


(defmethod mm-decode oid/CHAR_ARRAY
  [_ ^bytes buf opt]
  (decode-array buf opt))


(defn decode
  ([oid buf]
   (mm-decode oid buf nil))
  ([oid buf options]
   (mm-decode oid buf options)))
