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


;; TODO
(def ^DateTimeFormatter
  frmt-timestamptz
  (-> "yyyy-MM-dd HH:mm:ss[.n]x"
      (DateTimeFormatter/ofPattern)
      (.withZone ZoneOffset/UTC)))


;; TODO
(def ^DateTimeFormatter
  frmt-timestamp
  (-> "yyyy-MM-dd HH:mm:ss[.n]"
      (DateTimeFormatter/ofPattern)))


(def ^DateTimeFormatter
  frmt-timetz
  (-> (new DateTimeFormatterBuilder)
      (.appendPattern "HH:mm:ss")
      (.appendFraction ChronoField/MICRO_OF_SECOND 0 6 true)
      (.appendPattern "x")
      (.toFormatter)
      (.withZone ZoneOffset/UTC)))


;; TODO
(defn parse-timestampz ^Instant [^String string opt]
  (->> string
       (.parse frmt-timestamptz)
       (Instant/from)))


;; TODO
(defn parse-timestamp ^Instant [^String string opt]
  (let [ldt
        (->> string
             (.parse frmt-timestamp)
             (LocalDateTime/from))]
    (.toInstant ldt ZoneOffset/UTC)))


;; TODO
(defn parse-date ^Instant [^String string opt]
  (-> string
      (LocalDate/parse)
      (.atStartOfDay ZoneOffset/UTC)
      (.toInstant)))


;; TODO
(defn parse-timetz [^String string opt]
  (OffsetTime/parse string frmt-timetz))


;; TODO
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
