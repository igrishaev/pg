(ns pg.joda-time.encode.bin
  (:import
   java.util.TimeZone
   org.joda.time.Days
   org.joda.time.DateTime
   org.joda.time.LocalDate
   org.joda.time.LocalTime)
  (:require
   [pg.const :as c]
   [pg.bytes.array :as array]
   [pg.encode.bin :refer [-default -encode]]
   [pg.oid :as oid]))


(def ^LocalDate LD_EPOCH
  (new LocalDate 1970 1 1))


;;
;; LocalDate
;;

(-default LocalDate oid/date)


(defmethod -encode [LocalDate oid/date]
  [^LocalDate value _ _]
  (let [days
        (-> (Days/daysBetween LD_EPOCH value)
            (.getDays)
            (- (.toDays c/PG_EPOCH_DIFF)))]
    (array/arr32 days)))


;;
;; LocalTime
;;

(-default LocalTime oid/time)


(defmethod -encode [LocalTime oid/time]
  [^LocalTime value _ _]
  (array/arr64 (.getMillisOfDay value)))


;;
;; DateTime
;;

(-default DateTime oid/timestamp)


(defmethod -encode [DateTime oid/timestamp]
  [^DateTime value _ _]
  (let [millis
        (- (.getMillis value)
           (.toMillis c/PG_EPOCH_DIFF))

        offset-millis
        (.getRawOffset (TimeZone/getDefault))]

    (array/arr64
     (-> (* millis 1000)
         (+ (* offset-millis 1000))))))