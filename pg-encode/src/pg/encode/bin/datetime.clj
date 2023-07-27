(ns pg.encode.bin.datetime
  (:require
   [pg.const :as const]
   [pg.bytes :as bytes]
   [pg.bb :as bb]
   [pg.oid :as oid])
  (:import
   java.time.temporal.ChronoField
   java.time.OffsetDateTime
   java.time.Duration
   java.time.Instant
   java.time.ZonedDateTime
   java.time.LocalTime
   java.time.OffsetTime
   java.time.LocalDate
   java.time.ZoneId
   java.time.ZoneOffset
   java.util.Date
   java.util.TimeZone))


;; TODO

;; LocalDate-timestamp
;; LocalDate-timestamptz
;; ZonedDateTime-date
;; ZonedDateTime-timestamp
;; ZonedDateTime-timestamptz
;; LocalDateTime-date
;; LocalDateTime-timestamp
;; LocalDateTime-timestamptz
;; OffsetTime-time
;; LocalTime-timetz

;; OffsetDateTime-date
;; OffsetDateTime-timestamp
;; OffsetDateTime-timestamptz


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
  (let [local-date
        (LocalDate/ofInstant value ZoneOffset/UTC)]
    (LocalDate-date local-date opt)))


(defn OffsetDateTime-date ^bytes [^OffsetDateTime value opt]
  (-> value
      (.toInstant)
      (Instant-date opt)))


(defn Date-date ^bytes [^Date value opt]
  (let [millis
        (- (.getTime value)
           (.toMillis const/PG_EPOCH_DIFF))

        offset-minutes
        (.getTimezoneOffset value)

        nanos
        (- (* millis 1000)
           (* offset-minutes 60 const/MICROS))]

    (bytes/int64->bytes nanos)))


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
