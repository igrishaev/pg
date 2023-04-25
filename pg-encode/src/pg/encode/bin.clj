(ns pg.encode.bin
  (:import
   clojure.lang.Symbol
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


(defmacro -default [Type oid]
  `(defmethod -encode [~Type nil]
     [value# _# opt#]
     (-encode value# ~oid opt#)))


;;
;; Symbol
;;

(-default Symbol oid/TEXT)


(defmethod -encode [Symbol oid/TEXT]
  [value oid opt]
  (-encode (str value) oid opt))


(defmethod -encode [Symbol oid/VARCHAR]
  [value oid opt]
  (-encode (str value) oid opt))


;;
;; String
;;


(-default String oid/TEXT)


(defmethod -encode [String oid/TEXT]
  [^String value _ _]
  (.getBytes value "UTF-8"))


(defmethod -encode [String oid/VARCHAR]
  [value _ opt]
  (-encode value oid/TEXT opt))


;;
;; Character
;;

(-default Character oid/TEXT)


(defmethod -encode [Character oid/TEXT]
  [value oid opt]
  (-encode (str value) oid opt))


(defmethod -encode [Character oid/VARCHAR]
  [value oid opt]
  (-encode value oid/TEXT opt))


;;
;; Long
;;

(-default Long oid/INT8)


(defmethod -encode [Long oid/INT8]
  [value _ _]
  (array/arr64 value))


(defmethod -encode [Long oid/INT4]
  [value oid opt]
  (-encode (int value) oid opt))


(defmethod -encode [Long oid/INT2]
  [^Long value oid opt]
  (-encode (short value) oid opt))


;;
;; Integer
;;

(-default Integer oid/INT4)


(defmethod -encode [Integer oid/INT8]
  [value oid opt]
  (-encode (long value) oid opt))


(defmethod -encode [Integer oid/INT4]
  [value oid opt]
  (array/arr32 value))


(defmethod -encode [Integer oid/INT2]
  [value oid opt]
  (-encode (short value) oid opt))


;;
;; Short
;;

(-default Short oid/INT2)


(defmethod -encode [Short oid/INT8]
  [value oid opt]
  (-encode (long value) oid opt))


(defmethod -encode [Short oid/INT4]
  [value oid opt]
  (-encode (int value) oid opt))


(defmethod -encode [Short oid/INT2]
  [value oid opt]
  (array/arr16 value))


;;
;; Bool
;;

(-default Boolean oid/BOOL)


(defmethod -encode [Boolean oid/BOOL]
  [value _ _]
  (case value
    true (byte-array [(byte 1)])
    false (byte-array [(byte 0)])))


;;
;; Float
;;

(-default Float oid/FLOAT4)


(defmethod -encode [Float oid/FLOAT4]
  [value oid opt]
  (-> (Float/floatToIntBits value)
      (array/arr32)))


(defmethod -encode [Float oid/FLOAT8]
  [value oid opt]
  (-encode (double value) oid opt))


;;
;; Double
;;


(-default Double oid/FLOAT8)


(defmethod -encode [Double oid/FLOAT8]
  [value oid opt]
  (-> (Double/doubleToLongBits value)
      (array/arr64)))


(defmethod -encode [Double oid/FLOAT4]
  [value oid opt]
  (-encode (float value) oid opt))


;;
;; UUID
;;

(-default UUID oid/UUID)


(defmethod -encode [UUID oid/UUID]
  [^UUID value oid opt]

  (let [most-bits
        (.getMostSignificantBits value)

        least-bits
        (.getLeastSignificantBits value)]

    (byte-array
     (-> []
         (into (array/arr64 most-bits))
         (into (array/arr64 least-bits))))))


(defmethod -encode [String oid/UUID]
  [value oid opt]
  (-encode (UUID/fromString value) oid opt))


(defmethod -encode [UUID oid/TEXT]
  [value oid opt]
  (-encode (str value) oid opt))


;;
;; Instant
;;

(-default Instant oid/TIMESTAMP)


(defmethod -encode [Instant oid/TIMESTAMP]
  [^Instant value _ _]

  (let [seconds
        (- (.getEpochSecond value)
           (.toSeconds c/PG_EPOCH_DIFF))

        nanos
        (.getNano value)]

    (array/arr64
     (+ (* seconds 1000 1000)
        (quot nanos 1000)))))


(defmethod -encode [Instant oid/DATE]
  [^Instant value oid opt]
  (let [local-date
        (LocalDate/ofInstant value
                             (ZoneId/systemDefault))]
    (-encode local-date oid opt)))


;;
;; Date
;;

(-default Date oid/TIMESTAMP)


(defmethod -encode [Date oid/DATE]
  [^Date value oid opt]
  (let [local-date
        (LocalDate/ofInstant (.toInstant value)
                             (ZoneId/systemDefault))]
    (-encode local-date oid opt)))


(defmethod -encode [Date oid/TIMESTAMP]
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

(-default LocalDate oid/DATE)


(defmethod -encode [LocalDate oid/DATE]
  [^LocalDate value oid opt]
  (let []
    (array/arr32
     (- (.toEpochDay value)
        (.toDays c/PG_EPOCH_DIFF)))))


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
