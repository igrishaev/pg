(ns pg.encode.txt.datetime
  (:import
   java.time.Instant
   java.time.LocalDate
   java.time.LocalTime
   java.time.OffsetTime
   java.time.ZoneId
   java.time.ZoneOffset
   java.time.ZonedDateTime
   java.time.OffsetDateTime
   java.time.LocalDateTime
   java.time.format.DateTimeFormatter
   java.util.Date)
  (:require
   [pg.encode.txt.core
    :refer [expand]]
   [pg.oid :as oid]))


(def ^DateTimeFormatter
  frmt-timestamptz
  (-> "yyyy-MM-dd HH:mm:ss.SSSSSSx"
      (DateTimeFormatter/ofPattern)
      (.withZone (ZoneId/of "UTC"))))


(def ^DateTimeFormatter
  frmt-date
  (-> "yyyy-MM-dd"
      (DateTimeFormatter/ofPattern)
      (.withZone (ZoneId/of "UTC"))))


(def ^DateTimeFormatter
  frmt-timestamp
  (-> "yyyy-MM-dd HH:mm:ss.SSSSSS"
      (DateTimeFormatter/ofPattern)
      (.withZone (ZoneId/of "UTC"))))


(def ^DateTimeFormatter
  frmt-timetz
  (-> "HH:mm:ss.SSSSSSx"
      (DateTimeFormatter/ofPattern)))


(def ^DateTimeFormatter
  frmt-time
  (-> "HH:mm:ss.SSSSSS"
      (DateTimeFormatter/ofPattern)))


(defn ZonedDateTime-timestamptz
  [^ZonedDateTime obj opt]
  (.format frmt-timestamptz obj))


(defn ZonedDateTime-date
  [^ZonedDateTime obj opt]
  (.format obj frmt-date))


(defn ZonedDateTime-timestamp
  [^ZonedDateTime obj opt]
  (.format frmt-timestamp obj))


(defn ZonedDateTime-time
  [^ZonedDateTime obj opt]
  (.format frmt-time obj))


(defn ZonedDateTime-timetz
  [^ZonedDateTime obj opt]
  (.format frmt-timetz obj))


(defn Instant-timestamptz
  [^Instant obj opt]
  (.format frmt-timestamptz obj))


(defn Date-timestamptz
  [^Date obj opt]
  (Instant-timestamptz (.toInstant obj) opt))


(defn Instant-timestamp
  [^Instant obj opt]
  (.format frmt-timestamp obj))


(defn Date-timestamp
  [^Date obj opt]
  (Instant-timestamp (.toInstant obj) opt))


(defn Instant-date
  [^Instant obj opt]
  (.format frmt-date obj))


(defn Date-date
  [^Date obj opt]
  (Instant-date (.toInstant obj) opt))


(defn LocalTime-time
  [^LocalTime obj opt]
  (.format obj frmt-time))


(defn LocalTime-timetz
  [^LocalTime obj opt]
  (-> obj
      (.atOffset ZoneOffset/UTC)
      (.format frmt-timetz)))


(defn OffsetTime-timetz
  [^OffsetTime obj opt]
  (.format obj frmt-timetz))


(defn OffsetTime-time
  [^OffsetTime obj opt]
  (-> obj
      (.withOffsetSameInstant ZoneOffset/UTC)
      (.format frmt-time)))


(defn LocalDate-date
  [^LocalDate obj opt]
  (.format obj frmt-date))


(defn LocalDate-timestamp
  [^LocalDate obj opt]
  (-> obj
      (.atStartOfDay)
      (.format frmt-timestamp)))


(defn LocalDate-timestamptz
  [^LocalDate obj opt]
  (-> obj
      (.atStartOfDay ZoneOffset/UTC)
      (.format frmt-timestamptz)))


(defn LocalDateTime-date [^LocalDateTime obj opt]
  (.format obj frmt-date))


(defn LocalDateTime-timestamp [^LocalDateTime obj opt]
  (.format obj frmt-timestamp))


(defn LocalDateTime-timestamptz [^LocalDateTime obj opt]
  (-> obj
      (.atZone ZoneOffset/UTC)
      (.format frmt-timestamptz)))


(defn OffsetDateTime-date [^OffsetDateTime obj opt]
  (.format obj frmt-date))


(defn OffsetDateTime-timestamp [^OffsetDateTime obj opt]
  (.format obj frmt-timestamp))


(defn OffsetDateTime-timestamptz [^OffsetDateTime obj opt]
  (.format obj frmt-timestamptz))


;;
;; Date & time
;;

(expand [Instant nil
         Instant oid/timestamptz]
  [value _ opt]
  (Instant-timestamptz value opt))


(expand [Instant oid/timestamp]
  [value _ opt]
  (Instant-timestamp value opt))


(expand [Instant oid/date]
  [value _ opt]
  (Instant-date value opt))


(expand [Date nil
         Date oid/timestamptz]
  [value _ opt]
  (Date-timestamptz value opt))


(expand [Date oid/timestamp]
  [value _ opt]
  (Date-timestamp value opt))


(expand [Date oid/date]
  [value _ opt]
  (Date-date value opt))


(expand [LocalTime nil
         LocalTime oid/time]
  [value _ opt]
  (LocalTime-time value opt))


(expand [LocalTime oid/timetz]
  [value _ opt]
  (LocalTime-timetz value opt))


(expand [OffsetTime nil
         OffsetTime oid/timetz]
  [value _ opt]
  (OffsetTime-timetz value opt))


(expand [OffsetTime oid/time]
  [value _ opt]
  (OffsetTime-time value opt))


(expand [ZonedDateTime nil
         ZonedDateTime oid/timestamptz]
  [value _ opt]
  (ZonedDateTime-timestamptz value opt))


(expand [ZonedDateTime oid/time]
  [value _ opt]
  (ZonedDateTime-time value opt))


(expand [ZonedDateTime oid/timetz]
  [value _ opt]
  (ZonedDateTime-timetz value opt))


(expand [ZonedDateTime oid/timestamp]
  [value _ opt]
  (ZonedDateTime-timestamp value opt))


(expand [ZonedDateTime oid/date]
  [value _ opt]
  (ZonedDateTime-date value opt))


(expand [LocalDateTime oid/date]
  [value _ opt]
  (LocalDateTime-date value opt))


(expand [LocalDateTime nil
         LocalDateTime oid/timestamp]
  [value _ opt]
  (LocalDateTime-timestamp value opt))


(expand [LocalDateTime oid/timestamptz]
  [value _ opt]
  (LocalDateTime-timestamptz value opt))


(expand [OffsetDateTime oid/date]
  [value _ opt]
  (OffsetDateTime-date value opt))


(expand [OffsetDateTime oid/timestamp]
  [value _ opt]
  (OffsetDateTime-timestamp value opt))


(expand [OffsetDateTime nil
         OffsetDateTime oid/timestamptz]
  [value _ opt]
  (OffsetDateTime-timestamptz value opt))


(expand [LocalDate nil
         LocalDate oid/date]
  [value _ opt]
  (LocalDate-date value opt))


(expand [LocalDate oid/timestamp]
  [value _ opt]
  (LocalDate-timestamp value opt))


(expand [LocalDate oid/timestamptz]
  [value _ opt]
  (LocalDate-timestamptz value opt))
