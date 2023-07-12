(ns pg.encode.txt.datetime
  (:import
   java.util.Date
   java.time.ZoneId
   java.time.ZonedDateTime
   java.time.ZoneOffset
   java.time.Instant
   java.time.format.DateTimeFormatter))


(def ^DateTimeFormatter
  frmt-timestamptz
  (-> "yyyy-MM-dd HH:mm:ss.nx"
      (DateTimeFormatter/ofPattern)
      (.withZone (ZoneId/of "UTC"))))


(def ^DateTimeFormatter
  frmt-date
  (-> "yyyy-MM-dd"
      (DateTimeFormatter/ofPattern)
      (.withZone (ZoneId/of "UTC"))))


(def ^DateTimeFormatter
  frmt-timestamp
  (-> "yyyy-MM-dd HH:mm:ss"
      (DateTimeFormatter/ofPattern)
      (.withZone (ZoneId/of "UTC"))))


(def ^DateTimeFormatter
  frmt-timetz
  (-> "HH:mm:ss.nx"
      (DateTimeFormatter/ofPattern)))


(def ^DateTimeFormatter
  frmt-time
  (-> "HH:mm:ss.n"
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


(comment

  (def -ldt
    (ZonedDateTime/of 1985 12 31 23 59 59 0 (ZoneId/of "UTC")))

  (def -inst
    (Instant/now))

  (def -d
    (new Date))

  (def -sql
    (new java.sql.Timestamp (- 1985 1900) 11 31 23 59 59 999))

  (ZonedDateTime-timestamptz -ldt nil)
  (ZonedDateTime-timestamp -ldt nil)
  (ZonedDateTime-date -ldt nil)
  (ZonedDateTime-time -ldt nil)
  (ZonedDateTime-timetz -ldt nil)

  (Instant-timestamptz -inst nil)
  (Instant-timestamp -inst nil)
  (Instant-date -inst nil)

  (Date-timestamptz -d nil)
  (Date-timestamp -d nil)
  (Date-date -d nil)

  (Date-date -sql nil)




  )
