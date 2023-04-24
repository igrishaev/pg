(ns pg.const
  (:import
   java.time.Duration
   java.time.Instant
   java.time.LocalDate
   java.time.ZoneOffset))


(def ^Duration PG_EPOCH_DIFF
  (Duration/between Instant/EPOCH
                    (-> (LocalDate/of 2000 1 1)
                        (.atStartOfDay)
                        (.toInstant ZoneOffset/UTC))))
