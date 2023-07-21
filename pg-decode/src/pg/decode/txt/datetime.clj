(ns pg.decode.txt.datetime
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


(def ^DateTimeFormatter
  frmt-timestamptz
  (-> (new DateTimeFormatterBuilder)
      (.appendPattern "yyyy-MM-dd HH:mm:ss")
      (.appendFraction ChronoField/MICRO_OF_SECOND 0 6 true)
      (.appendPattern "x")
      (.toFormatter)
      (.withZone ZoneOffset/UTC)))


(def ^DateTimeFormatter
  frmt-timestamp
  (-> (new DateTimeFormatterBuilder)
      (.appendPattern "yyyy-MM-dd HH:mm:ss")
      (.appendFraction ChronoField/MICRO_OF_SECOND 0 6 true)
      (.toFormatter)))


(def ^DateTimeFormatter
  frmt-timetz
  (-> (new DateTimeFormatterBuilder)
      (.appendPattern "HH:mm:ss")
      (.appendFraction ChronoField/MICRO_OF_SECOND 0 6 true)
      (.appendPattern "x")
      (.toFormatter)
      (.withZone ZoneOffset/UTC)))


(defn parse-timestampz ^Instant [^String string opt]
  (->> string
       (.parse frmt-timestamptz)
       (Instant/from)))


(defn parse-timestamp ^Instant [^String string opt]
  (let [ldt
        (->> string
             (.parse frmt-timestamp)
             (LocalDateTime/from))]
    (.toInstant ldt ZoneOffset/UTC)))


(defn parse-date ^Instant [^String string opt]
  (-> string
      (LocalDate/parse)
      (.atStartOfDay ZoneOffset/UTC)
      (.toInstant)))


(defn parse-timetz [^String string opt]
  (OffsetTime/parse string frmt-timetz))


(defn parse-time [^String string opt]
  (LocalTime/parse string))



(comment

  (parse-timestampz "2023-07-10 22:25:22.046553+03" nil)

  (parse-timestampz "2022-07-03 00:00:00+03" nil)

  (parse-timestamp "2022-07-03 00:00:00" nil)

  (parse-timestamp "2022-01-01 23:59:59.123" nil)

  (parse-date "2023-07-11" nil)

  (parse-timetz "10:29:39.853741+03" nil)

  (parse-timetz "10:29:39+03" nil)

  (parse-time "10:29:39" nil)

  )
