(ns pg.client.acc
  (:import
   java.util.ArrayList)
  )


(defprotocol IAcc

  (-init [this])

  (-reduce [this acc row])

  (-finalize [this result]))


(def acc-transient-vec

  (reify

    IAcc

    (-init [_]
      (transient []))

    (-reduce [_ acc! row]
      (conj! acc! row))

    (-finalize [_ acc!]
      (persistent! acc!))))


(def acc-array-list

  (reify

    IAcc

    (-init [_]
      (new ArrayList))

    (-reduce [_ acc row]
      (doto ^ArrayList acc (.add row)))

    (-finalize [_ acc]
      acc)))


(defn acc-index-by [f]

  (reify

    IAcc

    (-init [_]
      (transient {}))

    (-reduce [_ acc! row]
      (assoc! acc! (f row) row))

    (-finalize [_ acc!]
      (persistent! acc!))))


(defn acc-group-by [f]

  (let [-conj (fnil conj [])]

    (reify

      IAcc

      (-init [_]
        {})

      (-reduce [_ acc row]
        (update acc (f row) -conj row))

      (-finalize [_ result]
        result))))
