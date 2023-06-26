(ns pg.encode.bin
  (:refer-clojure :exclude [extend])
  (:import
   clojure.lang.MultiFn
   clojure.lang.Symbol
   java.time.Duration
   java.time.Instant
   java.time.LocalDate
   java.time.ZoneId
   java.time.ZoneOffset
   java.util.Date
   java.util.TimeZone
   java.util.UUID)
  (:require
   [clojure.template :refer [do-template]]
   [pg.bytes.array :as array]
   [pg.const :as c]
   [pg.oid :as oid]))


(defmulti -encode
  (fn [value oid _]
    [(type value) oid]))


(defmacro extend
  {:style/indent 1}
  [type-oid's binding & body]
  `(do-template [Type# oid#]
                (defmethod -encode [Type# oid#]
                  ~binding
                  ~@body)
                ~@type-oid's))


(defmethod -encode :default
  [value oid opt]
  (throw (ex-info "Cannot binary encode a value"
                  {:value value
                   :oid oid
                   :opt opt})))


;;
;; Symbol
;;

(extend [Symbol nil
         Symbol oid/text
         Symbol oid/varchar]
  [value oid opt]
  (-encode (str value) oid opt))


;;
;; String
;;

(extend [String nil
         String oid/text
         String oid/varchar]
  [^String value oid opt]
  (.getBytes value "UTF-8"))


;;
;; Character
;;

(extend [Character nil
         Character oid/text
         Character oid/varchar]
  [^Character value oid opt]
  (.getBytes (str value) "UTF-8"))


;;
;; Long
;;

(extend [Long nil
         Long oid/int8]
  [value oid opt]
  (array/arr64 value))

(extend [Long oid/int4]
  [value oid opt]
  (array/arr32 (int value)))

(extend [Long oid/int2]
  [value oid opt]
  (array/arr16 (short value)))


;;
;; Integer
;;

(extend [Integer oid/int8]
  [value oid opt]
  (array/arr64 (long value)))


(extend [Integer nil
         Integer oid/int4]
  [value oid opt]
  (array/arr32 value))


(extend [Integer oid/int2]
  [value oid opt]
  (array/arr16 (short value)))


;;
;; Short
;;

(extend [Short oid/int8]
  [value oid opt]
  (array/arr64 (long value)))

(extend [Short oid/int4]
  [value oid opt]
  (array/arr32 (int value)))

(extend [Short nil
         Short oid/int2]
  [value oid opt]
  (array/arr16 value))


;;
;; Bool
;;

(extend [Boolean nil
         Boolean oid/bool]
  [value oid opt]
  (case value
    true (byte-array [(byte 1)])
    false (byte-array [(byte 0)])))


;;
;; Float
;;

(extend [Float nil
         Float oid/float4]
  [value oid opt]
  (-> (Float/floatToIntBits value)
      (array/arr32)))

(extend [Float oid/float8]
  [value oid opt]
  (-> (Double/doubleToLongBits (double value))
      (array/arr64)))


;;
;; Double
;;

(extend [Double nil
         Double oid/float8]
  [value oid opt]
  (-> (Double/doubleToLongBits value)
      (array/arr64)))

(extend [Double oid/float4]
  [value oid opt]
  (-> (Float/floatToIntBits (float value))
      (array/arr32)))


;;
;; UUID
;;

(extend [UUID nil
         UUID oid/uuid]
  [value oid opt]
  (let [most-bits
        (.getMostSignificantBits value)

        least-bits
        (.getLeastSignificantBits value)]

    (byte-array
     (-> []
         (into (array/arr64 most-bits))
         (into (array/arr64 least-bits))))))

(extend [String oid/uuid]
  [value oid opt]
  (-encode (UUID/fromString value) oid opt))

(extend [UUID oid/text]
  [value oid opt]
  (-encode (str value) oid opt))


;;
;; Instant
;;

(extend [Instant nil
         Instant oid/timestamp]
  [^Instant value oid opt]

  (let [seconds
        (- (.getEpochSecond value)
           (.toSeconds c/PG_EPOCH_DIFF))

        offset-millis
        (.getRawOffset (TimeZone/getDefault))

        nanos
        (.getNano value)]

    (array/arr64
     (-> (* seconds 1000 1000)
         (+ (quot nanos 1000))
         (+ (* offset-millis 1000))))))

(extend [Instant oid/date]
  [^Date value oid opt]
  (let [local-date
        (LocalDate/ofInstant value (ZoneId/systemDefault))]
    (-encode local-date oid opt)))


;;
;; Date
;;

(extend [Date oid/date]
  [^Date value oid opt]
  (let [local-date
        (LocalDate/ofInstant (.toInstant value)
                             (ZoneId/systemDefault))]
    (-encode local-date oid opt)))

(extend [Date nil
         Date oid/timestamp]
  [^Date value oid opt]
  (let [millis
        (- (.getTime value)
           (.toMillis c/PG_EPOCH_DIFF))

        offset-minutes
        (.getTimezoneOffset value)

        nanos
        (- (* millis 1000)
           (* offset-minutes 60 1000 1000))]

    (array/arr64 nanos)))


;;
;; LocalDate
;;

(extend [LocalDate nil
         LocalDate oid/date]
  [^LocalDate value oid opt]
  (array/arr32
   (- (.toEpochDay value)
      (.toDays c/PG_EPOCH_DIFF))))


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

;; clojure.lang.BigInt
;; BigInteger
