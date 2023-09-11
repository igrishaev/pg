(ns pg.decode.txt.datetime
  (:require
   [pg.oid :as oid]
   [pg.decode.txt.core :refer [expand]])
  (:import
   java.time.LocalDate
   java.time.LocalDateTime
   java.time.LocalTime
   java.time.OffsetDateTime
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


(def ^DateTimeFormatter
  frmt-date
  (-> (new DateTimeFormatterBuilder)
      (.appendPattern "yyyy-MM-dd")
      (.toFormatter)))


(defn parse-timestampz ^OffsetDateTime [^String string opt]
  (OffsetDateTime/parse string frmt-timestamptz))


(defn parse-timestamp ^LocalDateTime [^String string opt]
  (LocalDateTime/parse string frmt-timestamp))


(defn parse-date ^LocalDate [^String string opt]
  (LocalDate/parse string frmt-date))


(defn parse-timetz [^String string opt]
  (OffsetTime/parse string frmt-timetz))


(defn parse-time [^String string opt]
  (LocalTime/parse string))


;;
;; Date & time
;;

(expand [oid/timestamptz]
  [string _ opt]
  (parse-timestampz string opt))


(expand [oid/timestamp]
  [string _ opt]
  (parse-timestamp string opt))


(expand [oid/date]
  [string _ opt]
  (parse-date string opt))


(expand [oid/timetz]
  [string _ opt]
  (parse-timetz string opt))


(expand [oid/time]
  [string _ opt]
  (parse-time string opt))
