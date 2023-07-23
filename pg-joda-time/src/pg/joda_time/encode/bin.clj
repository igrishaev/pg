(ns pg.joda-time.encode.bin
  (:import
   java.util.TimeZone
   org.joda.time.Days
   org.joda.time.DateTime
   org.joda.time.LocalDate
   org.joda.time.LocalTime)
  (:require
   [pg.const :as const]
   [pg.bytes :as bytes]
   [pg.encode.bin :refer [expand]]
   [pg.oid :as oid]))


(def ^LocalDate LD_EPOCH
  (new LocalDate 1970 1 1))


;;
;; LocalDate
;;

(expand [LocalDate nil
         LocalDate oid/date]
  [^LocalDate value _ _]
  (let [days
        (-> (Days/daysBetween LD_EPOCH value)
            (.getDays)
            (- (.toDays const/PG_EPOCH_DIFF)))]
    (bytes/int32->bytes days)))


;;
;; LocalTime
;;

(expand [LocalTime nil
         LocalTime oid/time]
  [^LocalTime value _ _]
  (bytes/int64->bytes (.getMillisOfDay value)))


;;
;; DateTime
;;

(expand [DateTime nil
         DateTime oid/timestamp]
  [^DateTime value _ _]
  (let [millis
        (- (.getMillis value)
           (.toMillis const/PG_EPOCH_DIFF))

        offset-millis
        (.getRawOffset (TimeZone/getDefault))]

    (bytes/int64->bytes
     (-> (* millis 1000)
         (+ (* offset-millis 1000))))))
