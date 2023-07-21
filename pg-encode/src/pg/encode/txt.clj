(ns pg.encode.txt
  (:require
   [clojure.template :refer [do-template]]
   [pg.encode.txt.datetime :as datetime]
   [pg.oid :as oid])
  (:import
   clojure.lang.BigInt
   clojure.lang.Symbol
   java.math.BigDecimal
   java.math.BigInteger
   java.time.Instant
   java.time.LocalTime
   java.time.OffsetTime
   java.time.ZonedDateTime
   java.util.Date
   java.util.Formatter
   java.util.UUID))


(defmulti -encode
  (fn [value oid _]
    [(type value) oid]))


(defmethod -encode :default
  [value oid opt]
  (throw (ex-info "Cannot text-encode a value"
                  {:value value
                   :oid oid
                   :opt opt})))


(defmacro expand
  {:style/indent 1}
  [type-oid's binding & body]
  `(do-template [Type# oid#]
                (defmethod -encode [Type# oid#]
                  ~binding
                  ~@body)
                ~@type-oid's))



;;
;; Symbol
;;

(expand [Symbol nil
         Symbol oid/text
         Symbol oid/varchar]
  [value oid opt]
  (str value))


;;
;; String
;;

(expand [String nil
         String oid/text
         String oid/varchar]
  [^String value oid opt]
  value)


;;
;; Character
;;

(expand [Character nil
         Character oid/text
         Character oid/varchar]
  [^Character value oid opt]
  (str value))


;;
;; Long, Integer, Short
;; Double, Float
;; BigDecimal, BigInteger, BigInt
;;

(expand [Long nil
         Long oid/int8
         Long oid/int4
         Long oid/int2

         Integer nil
         Integer oid/int8
         Integer oid/int4
         Integer oid/int2

         Short nil
         Short oid/int8
         Short oid/int4
         Short oid/int2

         Double nil
         Double oid/float8
         Double oid/float4

         Float nil
         Float oid/float8
         Float oid/float4

         BigDecimal nil
         BigDecimal oid/numeric

         BigInteger nil
         BigInteger oid/numeric

         BigInt nil
         BigInt oid/numeric]
  [value _ _]
  (str value))


;;
;; Boolean
;;

(expand [Boolean nil
         Boolean oid/bool]
  [^Boolean value oid opt]
  (case value
    true "t"
    false "f"))


;;
;; UUID
;;

(expand [UUID nil
         UUID oid/uuid
         UUID oid/text
         UUID oid/varchar]
  [^UUID value oid opt]
  (str value))


;;
;; Date & time
;;

(expand [Instant nil
         Instant oid/timestamptz]
  [value _ opt]
  (datetime/Instant-timestamptz value opt))


(expand [Instant oid/timestamp]
  [value _ opt]
  (datetime/Instant-timestamp value opt))


(expand [Instant oid/date]
  [value _ opt]
  (datetime/Instant-date value opt))


(expand [Date nil
         Date oid/timestamptz]
  [value _ opt]
  (datetime/Date-timestamptz value opt))


(expand [Date oid/timestamp]
  [value _ opt]
  (datetime/Date-timestamp value opt))


(expand [Date oid/date]
  [value _ opt]
  (datetime/Date-date value opt))


(expand [LocalTime nil
         LocalTime oid/time]
  [value _ opt]
  (datetime/LocalTime-time value opt))


(expand [OffsetTime nil
         OffsetTime oid/timetz]
  [value _ opt]
  (datetime/OffsetTime-time value opt))


(expand [ZonedDateTime nil
         ZonedDateTime oid/timestamptz]
  [value _ opt]
  (datetime/ZonedDateTime-timestamptz value opt))


(expand [ZonedDateTime oid/time]
  [value _ opt]
  (datetime/ZonedDateTime-time value opt))


(expand [ZonedDateTime oid/timetz]
  [value _ opt]
  (datetime/ZonedDateTime-timetz value opt))


(expand [ZonedDateTime oid/timestamp]
  [value _ opt]
  (datetime/ZonedDateTime-timestamp value opt))


(expand [ZonedDateTime oid/date]
  [value _ opt]
  (datetime/ZonedDateTime-date value opt))


;;
;; API
;;

(defn encode
  (^String [value]
   (-encode value nil nil))

  (^String [value oid]
   (-encode value oid nil))

  (^String [value oid opt]
   (-encode value oid opt)))
