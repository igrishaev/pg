(ns pg.types.time)


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
