(ns pg.encode.bin
  (:import
   clojure.lang.Symbol
   java.time.Duration
   java.time.Instant
   java.time.LocalTime
   java.time.OffsetTime
   java.time.LocalDate
   java.time.ZoneId
   java.time.ZoneOffset
   java.util.Date
   java.util.TimeZone
   java.util.UUID)
  (:require
   [pg.encode.bin.datetime :as datetime]
   [clojure.template :refer [do-template]]
   [pg.bytes :as bytes]
   [pg.const :as const]
   [pg.oid :as oid]))


(defmulti -encode
  (fn [value oid _]
    [(type value) oid]))


(defmacro expand
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


(defn get-encoding ^String [options]
  (get options :client-encoding "UTF-8"))


;;
;; Symbol
;;

(expand [Symbol nil
         Symbol oid/text
         Symbol oid/varchar]
  [value oid opt]
  (-encode (str value) oid opt))


;;
;; String
;;

(expand [String nil
         String oid/text
         String oid/varchar]
  [^String value oid opt]
  (.getBytes value ^String (get-encoding opt)))


;;
;; Character
;;

(expand [Character nil
         Character oid/text
         Character oid/varchar]
  [^Character value oid opt]
  (.getBytes (str value) (get-encoding opt)))


;;
;; Long
;;

(expand [Long nil
         Long oid/int8]
  [value oid opt]
  (bytes/int64->bytes value))

(expand [Long oid/int4]
  [value oid opt]
  (bytes/int32->bytes (int value)))

(expand [Long oid/int2]
  [value oid opt]
  (bytes/int16->bytes (short value)))


;;
;; Integer
;;

(expand [Integer oid/int8]
  [value oid opt]
  (bytes/int64->bytes (long value)))


(expand [Integer nil
         Integer oid/int4]
  [value oid opt]
  (bytes/int32->bytes value))


(expand [Integer oid/int2]
  [value oid opt]
  (bytes/int16->bytes (short value)))


;;
;; Short
;;

(expand [Short oid/int8]
  [value oid opt]
  (bytes/int64->bytes (long value)))

(expand [Short oid/int4]
  [value oid opt]
  (bytes/int32->bytes (int value)))

(expand [Short nil
         Short oid/int2]
  [value oid opt]
  (bytes/int16->bytes value))


;;
;; Bool
;;

(expand [Boolean nil
         Boolean oid/bool]
  [value oid opt]
  (case value
    true (byte-array [(byte 1)])
    false (byte-array [(byte 0)])))


;;
;; Float
;;

(expand [Float nil
         Float oid/float4]
  [value oid opt]
  (-> (Float/floatToIntBits value)
      (bytes/int32->bytes)))

(expand [Float oid/float8]
  [value oid opt]
  (-> (Double/doubleToLongBits (double value))
      (bytes/int64->bytes)))


;;
;; Double
;;

(expand [Double nil
         Double oid/float8]
  [value oid opt]
  (-> (Double/doubleToLongBits value)
      (bytes/int64->bytes)))

(expand [Double oid/float4]
  [value oid opt]
  (-> (Float/floatToIntBits (float value))
      (bytes/int32->bytes)))


;;
;; UUID
;;

(expand [UUID nil
         UUID oid/uuid]
  [^UUID value oid opt]
  (let [most-bits
        (.getMostSignificantBits value)

        least-bits
        (.getLeastSignificantBits value)]

    (byte-array
     (-> []
         (into (bytes/int64->bytes most-bits))
         (into (bytes/int64->bytes least-bits))))))

(expand [String oid/uuid]
  [value oid opt]
  (-encode (UUID/fromString value) oid opt))

(expand [UUID oid/text]
  [value oid opt]
  (-encode (str value) oid opt))


;;
;; Instant
;;

(expand [Instant nil
         Instant oid/timestamp]
  [value _ opt]
  (datetime/Instant-timestamp value opt))


(expand [Instant oid/date]
  [value _ opt]
  (datetime/Instant-date value opt))


;;
;; Date
;;

(expand [Date oid/date]
  [value oid opt]
  (datetime/Date-date value opt))


(expand [Date nil
         Date oid/timestamp]
  [value _ opt]
  (datetime/Date-timestamp value opt))


;;
;; LocalDate
;;

(expand [LocalDate nil
         LocalDate oid/date]
  [^LocalDate value _ opt]
  (datetime/LocalDate-date value opt))


;;
;; OffsetTime
;;

(expand [OffsetTime nil
         OffsetTime oid/timetz]
  [value _ opt]
  (datetime/OffsetTime-timetz value opt))


;;
;; LocalTime
;;

(expand [LocalTime nil
         LocalTime oid/time]
  [value _ opt]
  (datetime/LocalTime-time value opt))


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
