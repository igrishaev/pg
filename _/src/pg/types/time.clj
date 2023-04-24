(ns pg.types.time
  (:import
   java.time.Instant
   java.time.Duration
   java.time.LocalDate
   java.time.ZoneOffset))


(def MICROS 1000000)

(def MILLIS 1000)


(def ^Duration PG_EPOCH_DIFF
  (Duration/between Instant/EPOCH
                    (-> (LocalDate/of 2000 1 1)
                        (.atStartOfDay)
                        (.toInstant ZoneOffset/UTC))))


(defrecord Interval
    [micros days months])


(defn make-interval
  ([]
   (new Interval 0 0 0))

  ([micros]
   (new Interval micros 0 0))

  ([micros days]
   (new Interval micros days 0))

  ([micros days months]
   (new Interval micros days months)))
