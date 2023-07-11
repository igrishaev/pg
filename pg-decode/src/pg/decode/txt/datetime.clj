(ns pg.client.decode.txt.datetime
  (:import
   java.time.ZoneId
   java.time.LocalTime
   java.time.OffsetTime
   java.time.format.DateTimeFormatter))


(def ^DateTimeFormatter
  frmt-timestamptz
  (-> "yyyy-MM-dd HH:mm:ss[.n]x"
      (DateTimeFormatter/ofPattern)
      (.withZone ZoneOffset/UTC)))


(def ^DateTimeFormatter
  frmt-timestamp
  (-> "yyyy-MM-dd HH:mm:ss"
      (DateTimeFormatter/ofPattern)))


(def ^DateTimeFormatter
  frmt-timetz
  (-> "HH:mm:ss[.n]x"
      (DateTimeFormatter/ofPattern)
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
  (->> string
       (.parse frmt-timetz)
       (OffsetTime/from)))


(defn parse-time [^String string opt]
  (LocalTime/parse string))



(comment

  (parse-timestampz "2023-07-10 22:25:22.046553+03" nil)

  (parse-timestampz "2022-07-03 00:00:00+03" nil)

  (parse-timestamp "2022-07-03 00:00:00" nil)

  (parse-date "2023-07-11" nil)

  (parse-timetz "10:29:39.853741+03" nil)

  (parse-timetz "10:29:39+03" nil)

  (parse-time "10:29:39" nil)

  )
