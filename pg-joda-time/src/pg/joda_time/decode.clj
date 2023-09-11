(ns pg.joda-time.decode
  (:import
   java.time.ZoneOffset
   java.time.OffsetDateTime
   java.time.LocalDateTime
   java.time.LocalDate
   java.time.Instant
   org.joda.time.DateTime)
  (:require
   [pg.oid :as oid]
   [pg.decode.txt.datetime :as txt.datetime]
   [pg.decode.bin.datetime :as bin.datetime]
   [pg.decode.bin.core :as bin]
   [pg.decode.txt.core :as txt]))


(defmacro Instant->DateTime [obj]
  `(-> ~obj
       (.toEpochMilli)
       (DateTime.)))


;;
;; Bin
;;

(bin/expand [oid/timestamptz]
  [value oid opt]
  (-> value
      (bin.datetime/parse-timestamptz opt)
      (.toInstant)
      (Instant->DateTime)))


(bin/expand [oid/timestamp]
  [value oid opt]
  (-> value
      (bin.datetime/parse-timestamp opt)
      (.atOffset ZoneOffset/UTC)
      (.toInstant)
      (Instant->DateTime)))


(bin/expand [oid/date]
  [value oid opt]
  (-> value
      (bin.datetime/parse-date opt)
      (.atStartOfDay ZoneOffset/UTC)
      (.toOffsetDateTime)
      (.toInstant)
      (Instant->DateTime)))


;;
;; Txt
;;

(txt/expand [oid/timestamptz]
  [value oid opt]
  (-> value
      (txt.datetime/parse-timestampz opt)
      (.toInstant)
      (Instant->DateTime)))


(txt/expand [oid/timestamp]
  [value oid opt]
  (-> value
      (txt.datetime/parse-timestamp opt)
      (.atOffset ZoneOffset/UTC)
      (.toInstant)
      (Instant->DateTime)))


(txt/expand [oid/date]
  [value oid opt]
  (-> value
      (txt.datetime/parse-date opt)
      (.atStartOfDay ZoneOffset/UTC)
      (.toOffsetDateTime)
      (.toInstant)
      (Instant->DateTime)))
