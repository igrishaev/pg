(ns pg.types.geom)


(defrecord Point [x y])


(defn make-point [x y]
  (new Point x y))


(defrecord Circle [x y r])


(defn make-circle [x y r]
  (new Circle x y r))
