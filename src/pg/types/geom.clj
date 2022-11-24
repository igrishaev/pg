(ns pg.types.geom)

(defrecord Point [x y])

(defn make-point [x y]
  (new Point x y))
