(ns pg.encode.txt.datetime
  (:import
   java.time.Instant
   java.time.LocalTime
   java.time.OffsetTime
   java.time.ZoneId
   java.time.ZoneOffset
   java.time.ZonedDateTime
   java.time.format.DateTimeFormatter
   java.util.Date))


;; TODO
;; LocalDateTime-date
;; LocalDateTime-timestamp
;; LocalDateTime-timestamptz
;; OffsetDateTime-date
;; OffsetDateTime-timestamp
;; OffsetDateTime-timestamptz


(def ^DateTimeFormatter
  frmt-timestamptz
  (-> "yyyy-MM-dd HH:mm:ss.SSSSSSx"
      (DateTimeFormatter/ofPattern)
      (.withZone (ZoneId/of "UTC"))))


(def ^DateTimeFormatter
  frmt-date
  (-> "yyyy-MM-dd"
      (DateTimeFormatter/ofPattern)
      (.withZone (ZoneId/of "UTC"))))


(def ^DateTimeFormatter
  frmt-timestamp
  (-> "yyyy-MM-dd HH:mm:ss.SSSSSS"
      (DateTimeFormatter/ofPattern)
      (.withZone (ZoneId/of "UTC"))))


(def ^DateTimeFormatter
  frmt-timetz
  (-> "HH:mm:ss.SSSSSSx"
      (DateTimeFormatter/ofPattern)))


(def ^DateTimeFormatter
  frmt-time
  (-> "HH:mm:ss.SSSSSS"
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


(defn LocalTime-time
  [^LocalTime obj opt]
  (.format obj frmt-time))


(defn OffsetTime-timetz
  [^OffsetTime obj opt]
  (.format obj frmt-timetz))


(defn OffsetTime-time
  [^OffsetTime obj opt]
  (.format obj frmt-time))


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
