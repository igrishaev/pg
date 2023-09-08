(ns pg.hint
  (:require
   [pg.oid :as oid])
  (:import
   clojure.lang.BigInt
   java.math.BigInteger
   java.math.BigDecimal
   java.util.Map
   java.util.HashMap
   java.util.UUID
   java.util.Date
   java.time.Instant
   java.time.LocalDate
   java.time.LocalDateTime
   java.time.LocalTime
   java.time.OffsetTime))


(defprotocol IHint
  (hint [this]))


(extend-protocol IHint

  nil
  (hint [_] 0)

  Object
  (hint [_] 0)

  Short
  (hint [_] oid/int2)

  Integer
  (hint [_] oid/int4)

  Long
  (hint [_] oid/int8)

  Float
  (hint [_] oid/float4)

  Double
  (hint [_] oid/float8)

  String
  (hint [_] oid/text)

  Boolean
  (hint [_] oid/bool)

  UUID
  (hint [_] oid/uuid)

  Date
  (hint [_] oid/timestamptz)

  Instant
  (hint [_] oid/timestamptz)

  BigDecimal
  (hint [_] oid/numeric)

  BigInteger
  (hint [_] oid/numeric)

  BigInt
  (hint [_] oid/numeric)

  LocalDate
  (hint [_] oid/date)

  LocalDateTime
  (hint [_] oid/timestamp)

  LocalTime
  (hint [_] oid/time)

  OffsetTime
  (hint [_] oid/timetz))


(defmacro add-hint [Type oid]
  `(extend-protocol IHint
     ~Type
     (hint [_#] ~oid)))
