(ns pg.client.decode.txt.datetime
  (:import
   java.time.ZoneId
   java.time.ZonedDateTime
   java.time.LocalDateTime
   java.time.LocalDate
   java.time.LocalTime
   java.time.OffsetTime
   java.time.format.DateTimeFormatter))


(def ^DateTimeFormatter
  frmt-tsz-microsec
  (-> "yyyy-MM-dd HH:mm:ss.nx"
      (DateTimeFormatter/ofPattern)
      (.withZone (ZoneId/of "UTC"))))


(def ^DateTimeFormatter
  frmt-tsz
  (-> "yyyy-MM-dd HH:mm:ssx"
      (DateTimeFormatter/ofPattern)
      (.withZone (ZoneId/of "UTC"))))


(def ^DateTimeFormatter
  frmt-ts
  (-> "yyyy-MM-dd HH:mm:ss"
      (DateTimeFormatter/ofPattern)))


(def ^DateTimeFormatter
  frmt-timetz-microsec
  (-> "HH:mm:ss.nx"
      (DateTimeFormatter/ofPattern)
      (.withZone (ZoneId/of "UTC"))))


(def ^DateTimeFormatter
  frmt-timetz
  (-> "HH:mm:ssx"
      (DateTimeFormatter/ofPattern)
      (.withZone (ZoneId/of "UTC"))))


(defn parse-timestampz [^String string opt]

  (let [formatter
        (case (.length string)

          22
          frmt-tsz

          29
          frmt-tsz-microsec

          ;; else
          (throw
           (ex-info (format "cannot parse timestampz: %s" string)
                    {:opt opt
                     :string string})))]

    (->> string
         (.parse formatter)
         (ZonedDateTime/from))))


(defn parse-timestamp [^String string opt]
  (->> string
       (.parse frmt-ts)
       (LocalDateTime/from)))


(defn parse-date [^String string opt]
  (LocalDate/parse string))


(defn parse-timetz [^String string opt]

  (let [formatter
        (case (.length string)

          18
          frmt-timetz-microsec

          11
          frmt-timetz

          ;; else

          (throw
           (ex-info (format "cannot parse timez: %s" string)
                    {:opt opt
                     :string string})))]

    (->> string
         (.parse formatter)
         (OffsetTime/from))))


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
