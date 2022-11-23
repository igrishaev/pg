(ns pg.types.decode.binary
  "
  https://github.com/pgjdbc/pgjdbc/blob/master/pgjdbc/src/main/java/org/postgresql/util/ByteConverter.java#L124
  https://github.com/pgjdbc/pgjdbc/blob/master/pgjdbc/src/main/java/org/postgresql/jdbc/TimestampUtils.java
  https://github.com/pgjdbc/pgjdbc/blob/master/pgjdbc/src/main/java/org/postgresql/util/PGbytea.java
  https://www.postgresql.org/message-id/attachment/13504/arrayAccess.txt
  https://postgrespro.ru/docs/postgresql/14/runtime-config-client
  https://hackage.haskell.org/package/postgresql-binary-0.9/docs/src/PostgreSQL-Binary-Decoder.html
  https://github.com/postgres/postgres/blob/master/src/backend/utils/adt/varbit.c
  "
  (:import
   java.util.UUID
   java.time.Duration
   java.time.LocalDate
   java.time.LocalTime
   java.time.OffsetTime
   java.time.LocalDateTime
   java.time.ZoneOffset
   java.time.Instant)
  (:require
   [pg.error :as e]
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
  [[dim & dims]]
  (if dim
    (if (seq dims)
      (vec (repeat dim (new-matrix dims)))
      (vec (repeat dim nil)))
    (e/error! "Wrong matrix dimensions"
              {:dim dim
               :dims dims})))


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
  "
  https://github.com/pgjdbc/pgjdbc/blob/135be5a4395033a4ba23a1dd70ad76e0bd443a8d/pgjdbc/src/main/java/org/postgresql/jdbc/ArrayDecoding.java#L498
  "
  ([buf]
   (decode-array buf nil))

  ([buf opt]
   (let [bb
         (bb/wrap buf)

         levels
         (bb/read-int32 bb)

         has-nulls?
         (not= 0 (bb/read-int32 bb))

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

     ;; TODO: special cases for levels = 0 and 1

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
                  (assoc-in matrix path item))))))))


;; TODO: batch defmethod(s)

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


(defmethod mm-decode oid/DATE_ARRAY
  [_ ^bytes buf opt]
  (decode-array buf opt))


(defmethod mm-decode oid/TIME_ARRAY
  [_ ^bytes buf opt]
  (decode-array buf opt))


(defmethod mm-decode oid/TIMETZ_ARRAY
  [_ ^bytes buf opt]
  (decode-array buf opt))


(defmethod mm-decode oid/TIMESTAMP_ARRAY
  [_ ^bytes buf opt]
  (decode-array buf opt))


(defmethod mm-decode oid/TIMESTAMPTZ_ARRAY
  [_ ^bytes buf opt]
  (decode-array buf opt))


(def ^Duration PG_EPOCH_DIFF
  (Duration/between Instant/EPOCH
                    (-> (LocalDate/of 2000 1 1)
                        (.atStartOfDay)
                        (.toInstant ZoneOffset/UTC))))


(defmethod mm-decode oid/DATE
  [_ ^bytes buf opt]
  (let [bb
        (bb/wrap buf)

        days
        (bb/read-int32 bb)]

    (LocalDate/ofEpochDay
     (+ days (.toDays PG_EPOCH_DIFF)))))


;; TODO oid/TIME :check decimal/double
(defmethod mm-decode oid/TIME
  [_ ^bytes buf opt]
  (let [bb
        (bb/wrap buf)

        micros
        (bb/read-long8 bb)]

    (LocalTime/ofNanoOfDay (* micros 1000))))

;; TODO: check decimal/double
(defmethod mm-decode oid/TIMETZ
  [_ ^bytes buf opt]
  (let [bb
        (bb/wrap buf)

        micros
        (bb/read-long8 bb)

        offset
        (bb/read-int32 bb)]

    (OffsetTime/of
     (LocalTime/ofNanoOfDay (* micros 1000))
     (ZoneOffset/ofTotalSeconds (- offset)))))

;; TODO: check decimal/double
(defmethod mm-decode oid/TIMESTAMP
  [_ ^bytes buf opt]
  (let [bb
        (bb/wrap buf)

        micros
        (bb/read-long8 bb)

        secs
        (-> (quot micros 1000000)
            (+ (.toSeconds PG_EPOCH_DIFF)))

        nanos
        (-> (mod micros 1000000)
            (* 1000))]

    (LocalDateTime/ofEpochSecond secs
                                 nanos
                                 ZoneOffset/UTC)))


#_
(defmethod mm-decode oid/INTERVAL
  [_ ^bytes buf opt]

  micro days months

  (let [bb
        (bb/wrap buf)

        micros
        (bb/read-long8 bb)

        days
        (bb/read-int32 bb)

        months
        (bb/read-int32 bb)]




))





(defn decode
  ([oid buf]
   (mm-decode oid buf nil))
  ([oid buf options]
   (mm-decode oid buf options)))
