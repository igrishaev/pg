(ns pg.encode.bin-test
  (:import
   java.util.Date
   ;; java.time.Instant
   java.time.LocalDate
   java.time.LocalTime
   java.time.OffsetTime
   ;; java.time.ZonedDateTime
   java.time.OffsetDateTime
   ;; java.time.LocalDateTime
   ;; java.math.BigDecimal
   ;; java.math.BigInteger
   )
  (:require
   [pg.decode.bin :refer [decode]]
   [pg.bytes :as bytes]
   [clojure.string :as str]
   [pg.oid :as oid]
   [pg.encode.bin :refer [encode]]
   [clojure.test :refer [deftest is testing]]))


(deftest test-numbers

  ;; int

  (let [res (encode 1)]
    (is (bytes/== (byte-array [0 0 0 0 0 0 0 1]) res)))

  (let [res (encode (int 1))]
    (is (bytes/== (byte-array [0 0 0 1]) res)))

  (let [res (encode (short 1))]
    (is (bytes/== (byte-array [0 1]) res)))

  ;; byte

  (let [res (encode (byte 1))]
    (is (bytes/== (byte-array [0 1]) res)))

  (let [res (encode (byte 1) oid/int4)]
    (is (bytes/== (byte-array [0 0 0 1]) res)))

  (let [res (encode (byte 1) oid/int8)]
    (is (bytes/== (byte-array [0 0 0 0 0 0 0 1]) res)))

  ;; float

  (let [res (encode (float 1.1) oid/float4)]
    (is (bytes/== (byte-array [63, -116, -52, -51]) res)))

  (let [res (encode (double 1.1) oid/float8)]
    (is (bytes/== (byte-array [63, -15, -103, -103, -103, -103, -103, -102]) res))
    (is (= (double 1.1) (decode res oid/float8))))

  ;; int -> float

  (let [res (encode (short 1) oid/float4)]
    (is (bytes/== (byte-array [63, -128, 0, 0]) res)))

  (let [res (encode (int 1) oid/float4)]
    (is (bytes/== (byte-array [63, -128, 0, 0]) res)))

  (let [res (encode (long 1) oid/float4)]
    (is (bytes/== (byte-array [63, -128, 0, 0]) res)))

  (let [res (encode (short 1) oid/float8)]
    (is (bytes/== (byte-array [63, -16, 0, 0, 0, 0, 0, 0]) res)))

  (let [res (encode (int 1) oid/float8)]
    (is (bytes/== (byte-array [63, -16, 0, 0, 0, 0, 0, 0]) res)))

  (let [res (encode (long 1) oid/float8)]
    (is (bytes/== (byte-array [63, -16, 0, 0, 0, 0, 0, 0]) res))))


(deftest test-datetime

  (let [val1 (OffsetTime/now)
        buf (encode val1 oid/timetz)
        val2 (decode buf oid/timetz)]
    (is (= val1 val2)))

  (let [val1 (LocalDate/now)
        buf (encode val1 oid/date)
        val2 (decode buf oid/date)]
    (is (= val1 val2)))

  (let [val1 (LocalTime/now)
        buf (encode val1 oid/time)
        val2 (decode buf oid/time)]
    (is (= val1 val2)))

  (let [val1 (OffsetDateTime/now)
        buf (encode val1 oid/timestamptz)
        val2 (decode buf oid/timestamptz)]
    (is (= val1 val2)))

  )

;; TODO

;; biging
;; bigdec
;; numeric
