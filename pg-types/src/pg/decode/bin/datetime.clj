(ns pg.decode.bin.datetime
  (:require
   [pg.decode.bin.core :refer [expand]]
   [pg.bb :as bb]
   [pg.oid :as oid]
   [pg.const :as const])
  (:import
   java.time.Instant
   java.time.LocalDate
   java.time.LocalDateTime
   java.time.LocalTime
   java.time.OffsetTime
   java.time.OffsetDateTime
   java.time.ZoneOffset
   java.time.format.DateTimeFormatter
   java.time.format.DateTimeFormatterBuilder
   java.time.temporal.ChronoField))


(defn parse-timestamptz ^OffsetDateTime [^bytes buf opt]

  (let [bb
        (bb/wrap buf)

        micros
        (bb/read-int64 bb)

        secs
        (-> (quot micros const/MICROS)
            (+ (.toSeconds const/PG_EPOCH_DIFF)))

        nanos
        (-> (mod micros const/MICROS)
            (* const/MILLIS))

        instant
        (Instant/ofEpochSecond secs nanos)]

    (OffsetDateTime/ofInstant instant ZoneOffset/UTC)))


(defn parse-timestamp ^LocalDateTime [^bytes buf opt]

  (let [bb
        (bb/wrap buf)

        micros
        (bb/read-int64 bb)

        secs
        (-> (quot micros const/MICROS)
            (+ (.toSeconds const/PG_EPOCH_DIFF)))

        nanos
        (-> (rem micros const/MICROS)
            (* const/MILLIS))]

    (LocalDateTime/ofEpochSecond secs
                                 nanos
                                 ZoneOffset/UTC)))


(defn parse-date ^LocalDate [^bytes buf opt]

  (let [bb
        (bb/wrap buf)

        days
        (bb/read-int32 bb)]

    (LocalDate/ofEpochDay
     (+ days (.toDays const/PG_EPOCH_DIFF)))))


(defn parse-timetz ^OffsetTime [^bytes buf opt]

  (let [bb
        (bb/wrap buf)

        micros
        (bb/read-int64 bb)

        offset
        (bb/read-int32 bb)]

    (OffsetTime/of
     (LocalTime/ofNanoOfDay (* micros const/MILLIS))
     (ZoneOffset/ofTotalSeconds (- offset)))))


(defn parse-time ^LocalTime [^bytes buf opt]

  (let [bb
        (bb/wrap buf)

        micros
        (bb/read-int64 bb)]

    (LocalTime/ofNanoOfDay (* micros const/MILLIS))))


;;
;; Date & time
;;

(expand [oid/time]
  [^bytes buf _ opt]
  (parse-time buf opt))


(expand [oid/timetz]
  [^bytes buf _ opt]
  (parse-timetz buf opt))


(expand [oid/timestamp]
  [^bytes buf _ opt]
  (parse-timestamp buf opt))


(expand [oid/timestamptz]
  [^bytes buf _ opt]
  (parse-timestamptz buf opt))


(expand [oid/date]
  [^bytes buf _ opt]
  (parse-date buf opt))
