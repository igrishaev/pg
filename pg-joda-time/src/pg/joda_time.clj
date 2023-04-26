(ns pg.joda-time
  (:import
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

(-default LocalDate oid/DATE)


(defmethod -encode [LocalDate oid/DATE]
  [^LocalDate value _ _]
  (let [days
        (-> (Days/daysBetween LD_EPOCH value)
            (.getDays)
            (- (.toDays c/PG_EPOCH_DIFF)))]
    (array/arr32 days)))


;;
;; LocalTime
;;

(-default LocalTime oid/TIME)


(defmethod -encode [LocalTime oid/TIME]
  [^LocalTime value _ _]
  (array/arr64 (.getMillisOfDay value)))


;;
;; DateTime
;;

(-default DateTime oid/TIMESTAMP)


(defmethod -encode [DateTime oid/TIMESTAMP]
  [^DateTime value _ _]
  (let [millis
        (- (.getMillis value)
           (.toMillis c/PG_EPOCH_DIFF))

        sec-millis
        (.getMillisOfSecond value)]

    (array/arr64
     (-> (* millis 1000)
         (+ sec-millis)

         )
     #_
     (+ (- millis (.toMillis c/PG_EPOCH_DIFF))
        (* sec-millis 1000)))))
