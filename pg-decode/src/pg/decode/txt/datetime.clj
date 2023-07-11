(ns pg.client.decode.txt.datetime
  (:import
   java.time.ZoneId
   java.time.LocalDateTime
   java.time.LocalDate
   java.time.ZoneOffset
   java.time.LocalTime
   java.time.Instant
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
         (Instant/from))))


(defn parse-timestamp [^String string opt]
  (->> string
       (.parse frmt-ts)
       (LocalDateTime/from)))


(comment

  (parse-timestampz "2023-07-10 22:25:22.046553+03" nil)

  (parse-timestampz "2022-07-03 00:00:00+03" nil)

  (parse-timestamp "2022-07-03 00:00:00" nil)

  )
