(ns pg.encode.bin
  (:import
   clojure.lang.MultiFn
   clojure.lang.Symbol
   java.util.TimeZone
   java.time.Duration
   java.time.Instant
   java.time.LocalDate
   java.time.ZoneId
   java.time.ZoneOffset
   java.util.Date
   java.util.UUID)
  (:require
   [pg.const :as c]
   [pg.bytes.array :as array]
   [pg.error :as e]
   [pg.oid :as oid]))


(defmulti -encode
  (fn [value oid _]
    [(type value) oid]))


(defmethod -encode :default
  [value oid opt]
  (e/with-context
    {:value value
     :oid oid
     :opt opt}
    (e/error! "Cannot binary encode a value")))


(defn set-default [Type oid]
  (let [method
        (.getMethod ^MultiFn -encode [Type oid])

        default
        (.getMethod ^MultiFn -encode :default)]

    (if (or (nil? method) (= method default))
      (e/error! "There is no a method with [%s %s] dispatch value." Type oid)
      (.addMethod ^MultiFn -encode [Type nil] method))))


;;
;; Symbol
;;

(defmethod -encode [Symbol oid/text]
  [value oid opt]
  (-encode (str value) oid opt))


(defmethod -encode [Symbol oid/varchar]
  [value oid opt]
  (-encode (str value) oid opt))


(set-default Symbol oid/text)


;;
;; String
;;

(defmethod -encode [String oid/text]
  [^String value _ _]
  (.getBytes value "UTF-8"))


(defmethod -encode [String oid/varchar]
  [value _ opt]
  (-encode value oid/text opt))


(set-default String oid/text)

;;
;; Character
;;

(defmethod -encode [Character oid/text]
  [value oid opt]
  (-encode (str value) oid opt))


(defmethod -encode [Character oid/varchar]
  [value oid opt]
  (-encode value oid/text opt))


(set-default Character oid/text)


;;
;; Long
;;

(defmethod -encode [Long oid/int8]
  [value _ _]
  (array/arr64 value))


(defmethod -encode [Long oid/int4]
  [value oid opt]
  (-encode (int value) oid opt))


(defmethod -encode [Long oid/int2]
  [^Long value oid opt]
  (-encode (short value) oid opt))


(set-default Long oid/int8)


;;
;; Integer
;;

(defmethod -encode [Integer oid/int8]
  [value oid opt]
  (-encode (long value) oid opt))


(defmethod -encode [Integer oid/int4]
  [value oid opt]
  (array/arr32 value))


(defmethod -encode [Integer oid/int2]
  [value oid opt]
  (-encode (short value) oid opt))


(set-default Integer oid/int4)


;;
;; Short
;;

(defmethod -encode [Short oid/int8]
  [value oid opt]
  (-encode (long value) oid opt))


(defmethod -encode [Short oid/int4]
  [value oid opt]
  (-encode (int value) oid opt))


(defmethod -encode [Short oid/int2]
  [value oid opt]
  (array/arr16 value))


(set-default Short oid/int2)


;;
;; Bool
;;

(defmethod -encode [Boolean oid/bool]
  [value _ _]
  (case value
    true (byte-array [(byte 1)])
    false (byte-array [(byte 0)])))


(set-default Boolean oid/bool)


;;
;; Float
;;

(defmethod -encode [Float oid/float4]
  [value oid opt]
  (-> (Float/floatToIntBits value)
      (array/arr32)))


(defmethod -encode [Float oid/float8]
  [value oid opt]
  (-encode (double value) oid opt))


(set-default Float oid/float4)


;;
;; Double
;;

(defmethod -encode [Double oid/float8]
  [value oid opt]
  (-> (Double/doubleToLongBits value)
      (array/arr64)))


(defmethod -encode [Double oid/float4]
  [value oid opt]
  (-encode (float value) oid opt))


(set-default Double oid/float8)


;;
;; UUID
;;

(defmethod -encode [UUID oid/uuid]
  [^UUID value oid opt]

  (let [most-bits
        (.getMostSignificantBits value)

        least-bits
        (.getLeastSignificantBits value)]

    (byte-array
     (-> []
         (into (array/arr64 most-bits))
         (into (array/arr64 least-bits))))))


(defmethod -encode [String oid/uuid]
  [value oid opt]
  (-encode (UUID/fromString value) oid opt))


(defmethod -encode [UUID oid/text]
  [value oid opt]
  (-encode (str value) oid opt))


(set-default UUID oid/uuid)


;;
;; Instant
;;

(defmethod -encode [Instant oid/timestamp]
  [^Instant value _ _]

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


(defmethod -encode [Instant oid/date]
  [^Instant value oid opt]
  (let [local-date
        (LocalDate/ofInstant value
                             (ZoneId/systemDefault))]
    (-encode local-date oid opt)))


(set-default Instant oid/timestamp)


;;
;; Date
;;

(defmethod -encode [Date oid/date]
  [^Date value oid opt]
  (let [local-date
        (LocalDate/ofInstant (.toInstant value)
                             (ZoneId/systemDefault))]
    (-encode local-date oid opt)))


(defmethod -encode [Date oid/timestamp]
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


(set-default Date oid/timestamp)


;;
;; LocalDate
;;

(defmethod -encode [LocalDate oid/date]
  [^LocalDate value _ _]
  (array/arr32
   (- (.toEpochDay value)
      (.toDays c/PG_EPOCH_DIFF))))


(set-default LocalDate oid/date)


;;
;; API
;;

(defn encode
  (^bytes [value]
   (encode value nil nil))

  (^bytes [value oid]
   (encode value oid nil))

  (^bytes [value oid opt]
   (-encode value oid opt)))

;; clojure.lang.BigInt
;; BigInteger
