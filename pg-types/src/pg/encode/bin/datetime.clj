(ns pg.encode.bin.datetime
  (:require
   [pg.const :as const]
   [pg.bytes :as bytes]
   [pg.bb :as bb]
   [pg.oid :as oid]
   [pg.encode.bin.core :refer [expand
                               -encode]])
  (:import
   java.time.temporal.ChronoField
   java.time.OffsetDateTime
   java.time.Duration
   java.time.Instant
   java.time.LocalDateTime
   java.time.ZonedDateTime
   java.time.LocalTime
   java.time.OffsetTime
   java.time.LocalDate
   java.time.ZoneId
   java.time.ZoneOffset
   java.util.Date
   java.util.TimeZone))


(defn Instant-timestamp ^bytes [^Instant value opt]
  (let [seconds
        (- (.getEpochSecond value)
           (.toSeconds const/PG_EPOCH_DIFF))

        nanos
        (.getNano value)]

    (bytes/int64->bytes
     (-> (* seconds const/MICROS)
         (+ (quot nanos 1000))))))


(defn Instant-timestamptz ^bytes [^Instant value opt]

  (let [secs
        (.getLong value ChronoField/INSTANT_SECONDS)

        micros
        (.getLong value ChronoField/MICRO_OF_SECOND)

        result
        (-> secs
            (- (.toSeconds const/PG_EPOCH_DIFF))
            (* const/MICROS)
            (+ micros))]

    (bytes/int64->bytes result)))


(defn OffsetDateTime-timestamptz ^bytes
  [^OffsetDateTime value opt]
  (-> value
      (.toInstant)
      (Instant-timestamptz opt)))


(defn OffsetDateTime-timestamp ^bytes
  [^OffsetDateTime value opt]
  (-> value
      (.toInstant)
      (Instant-timestamp opt)))


(defn LocalDate-date ^bytes [^LocalDate value opt]
  (bytes/int32->bytes
   (- (.toEpochDay value)
      (.toDays const/PG_EPOCH_DIFF))))


(defn LocalDate-timestamp ^bytes [^LocalDate value opt]
  (-> value
      (.atStartOfDay ZoneOffset/UTC)
      (.toOffsetDateTime)
      (.toInstant)
      (Instant-timestamp opt)))


(defn LocalDate-timestamptz ^bytes [^LocalDate value opt]
  (-> value
      (.atStartOfDay ZoneOffset/UTC)
      (.toOffsetDateTime)
      (.toInstant)
      (Instant-timestamptz opt)))


(defn Instant-date ^bytes [^Instant value opt]
  (-> value
      (LocalDate/ofInstant ZoneOffset/UTC)
      (LocalDate-date opt)))


(defn OffsetDateTime-date ^bytes [^OffsetDateTime value opt]
  (-> value
      (.toInstant)
      (Instant-date opt)))


(defn Date-date ^bytes [^Date value opt]
  (-> value
      (.toInstant)
      (Instant-date opt)))


(defn Date-timestamp ^bytes [^Date value opt]
  (Instant-timestamp (.toInstant value) opt))


(defn Date-timestamptz ^bytes [^Date value opt]
  (Instant-timestamptz (.toInstant value) opt))


(defn OffsetTime-timetz ^bytes [^OffsetTime value opt]

  (let [micros
        (.getLong value ChronoField/MICRO_OF_DAY)

        offset
        (- (.getLong value ChronoField/OFFSET_SECONDS))]

    (-> (bb/allocate 12)
        (bb/write-int64 micros)
        (bb/write-int32 offset)
        (bb/array))))


(defn LocalTime-time ^bytes [^LocalTime value opt]

  (let [micros
        (.getLong value ChronoField/MICRO_OF_DAY)]

    (-> (bb/allocate 8)
        (bb/write-int64 micros)
        (bb/array))))


(defn LocalTime-timetz ^bytes [^LocalTime value opt]
  (-> value
      (.atOffset ZoneOffset/UTC)
      (OffsetTime-timetz opt)))


(defn OffsetTime-time ^bytes [^OffsetTime value opt]
  (LocalTime-time (.toLocalTime value) opt))


(defn ZonedDateTime-timestamptz ^bytes [^ZonedDateTime value opt]
  (-> value
      (.toOffsetDateTime)
      (OffsetDateTime-timestamptz opt)))


(defn ZonedDateTime-timestamp ^bytes [^ZonedDateTime value opt]
  (-> value
      (.toOffsetDateTime)
      (OffsetDateTime-timestamp opt)))


(defn ZonedDateTime-date ^bytes [^ZonedDateTime value opt]
  (-> value
      (.toOffsetDateTime)
      (OffsetDateTime-date opt)))


(defn LocalDateTime-timestamptz ^bytes [^LocalDateTime value opt]
  (-> value
      (.atOffset ZoneOffset/UTC)
      (OffsetDateTime-timestamptz opt)))


(defn LocalDateTime-timestamp ^bytes [^LocalDateTime value opt]
  (-> value
      (.atOffset ZoneOffset/UTC)
      (OffsetDateTime-timestamp opt)))


(defn LocalDateTime-date ^bytes [^LocalDateTime value opt]
  (-> value
      (.atOffset ZoneOffset/UTC)
      (OffsetDateTime-date opt)))


;;
;; Instant
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


;;
;; Date
;;

(expand [Date oid/date]
  [value oid opt]
  (Date-date value opt))


(expand [Date nil
         Date oid/timestamptz]
  [value _ opt]
  (Date-timestamptz value opt))


(expand [Date oid/timestamp]
  [value _ opt]
  (Date-timestamp value opt))


;;
;; OffsetDateTime
;;

(expand [OffsetDateTime nil
         OffsetDateTime oid/timestamptz]
  [value _ opt]
  (OffsetDateTime-timestamptz value opt))


(expand [OffsetDateTime oid/timestamp]
  [value _ opt]
  (OffsetDateTime-timestamp value opt))


(expand [OffsetDateTime oid/date]
  [value _ opt]
  (OffsetDateTime-date value opt))


;;
;; LocalDate
;;

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


;;
;; ZonedDateTime
;;

(expand [ZonedDateTime nil
         ZonedDateTime oid/timestamptz]
  [value _ opt]
  (ZonedDateTime-timestamptz value opt))


(expand [ZonedDateTime oid/timestamp]
  [value _ opt]
  (ZonedDateTime-timestamp value opt))


(expand [ZonedDateTime oid/date]
  [value _ opt]
  (ZonedDateTime-date value opt))


;;
;; OffsetTime
;;

(expand [OffsetTime nil
         OffsetTime oid/timetz]
  [value _ opt]
  (OffsetTime-timetz value opt))


(expand [OffsetTime oid/time]
  [value _ opt]
  (OffsetTime-time value opt))


;;
;; LocalDateTime
;;

(expand [LocalDateTime nil
         LocalDateTime oid/timestamptz]
  [value _ opt]
  (LocalDateTime-timestamptz value opt))


(expand [LocalDateTime oid/timestamp]
  [value _ opt]
  (LocalDateTime-timestamp value opt))


(expand [LocalDateTime oid/date]
  [value _ opt]
  (LocalDateTime-date value opt))


;;
;; LocalTime
;;

(expand [LocalTime nil
         LocalTime oid/time]
  [value _ opt]
  (LocalTime-time value opt))


(expand [LocalTime oid/timetz]
  [value _ opt]
  (LocalTime-timetz value opt))
