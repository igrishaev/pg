(ns pg.decode.bin.datetime
  (:require
   [pg.bb :as bb]
   [pg.const :as const])
  (:import
   java.time.Instant
   java.time.LocalDate
   java.time.LocalDateTime
   java.time.LocalTime
   java.time.OffsetTime
   java.time.ZoneOffset
   java.time.format.DateTimeFormatter
   java.time.format.DateTimeFormatterBuilder
   java.time.temporal.ChronoField))


(defn parse-timestamptz [^bytes buf opt]
  buf)


(defn parse-timestamp [^bytes buf opt]
  buf)


(defn parse-date [^bytes buf opt]
  buf)


(defn parse-timetz [^bytes buf opt]
  buf)


(defn parse-time [^bytes buf opt]

  (let [bb
        (bb/wrap buf)

        micros
        (bb/read-int64 bb)]

    (LocalTime/ofNanoOfDay (* micros const/MILLIS))))
