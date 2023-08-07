(ns pg.encode.hint
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


(def ^:private ^Map -HINTS
  (doto (new HashMap)
    (.put Long          oid/int8)
    (.put Integer       oid/int4)
    (.put Short         oid/int2)
    (.put Double        oid/float8)
    (.put Float         oid/float4)
    (.put String        oid/text)
    (.put Boolean       oid/bool)
    (.put UUID          oid/uuid)
    (.put Date          oid/timestamptz)
    (.put Instant       oid/timestamptz)
    (.put BigDecimal    oid/numeric)
    (.put BigInteger    oid/numeric)
    (.put BigInt        oid/numeric)
    (.put LocalDate     oid/date)
    (.put LocalDateTime oid/timestamp)
    (.put LocalTime     oid/time)
    (.put OffsetTime    oid/timetz)))


(defn hint [value]
  (or (.get -HINTS (type value)) 0))


(defn add-hint [Type oid]
  (.put -HINTS Type oid))
