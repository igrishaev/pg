(ns pg.encode.bin-test
  (:import
   java.util.Date
   ;; java.time.Instant
   ;; java.time.LocalDate
   ;; java.time.LocalTime
   ;; java.time.OffsetTime
   ;; java.time.ZonedDateTime
   ;; java.time.OffsetDateTime
   ;; java.time.LocalDateTime
   ;; java.math.BigDecimal
   ;; java.math.BigInteger
   )
  (:require
   [pg.bytes :as bytes]
   [clojure.string :as str]
   [pg.oid :as oid]
   [pg.encode.bin :refer [encode]]
   [clojure.test :refer [deftest is testing]]))


(deftest test-numbers

  (let [res (encode 1)]
    (is (bytes/== (byte-array [0, 0, 0, 0, 0, 0, 0, 1]) res)))

  (let [res (encode (int 1))]
    (is (bytes/== (byte-array [0, 0, 0, 1]) res)))

  (let [res (encode (short 1))]
    (is (bytes/== (byte-array [0, 1]) res)))

  (let [res (encode (byte 1))]
    (is (bytes/== (byte-array [1]) res)))
  )
