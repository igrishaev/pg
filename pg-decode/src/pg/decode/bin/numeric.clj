;; https://github.com/pgjdbc/r2dbc-postgresql/blob/main/src/main/java/io/r2dbc/postgresql/codec/NumericDecodeUtils.java

(ns pg.decode.bin.numeric
  (:require
   [pg.bb :as bb])
  (:import
   java.util.ArrayList
   java.math.RoundingMode
   java.math.BigDecimal))


(defn BigDecimal-numeric ^BigDecimal [^bytes buf opt]

  (let [bb
        (bb/wrap buf)

        ndigits
        (bb/read-int16 bb)]

    (if (zero? ndigits)

      BigDecimal/ZERO

      (let [weight
            (bb/read-int16 bb)

            sign
            (bb/read-int16 bb)

            scale
            (bb/read-int16 bb)

            digits
            (new ArrayList)

            sb
            (new StringBuilder)]

        (loop [i 0]
          (when-not (= i ndigits)
            (.add digits (bb/read-int16 bb))
            (recur (inc i))))

        (when-not (zero? sign)
          (.append sb "-"))

        (.append sb "0.")

        (loop [i 0]
          (when-not (= i ndigits)
            (let [digit (.get digits i)]
              (.append sb (format "%04d" digit)))
            (recur (inc i))))

        (-> (new BigDecimal (.toString sb))
            (.movePointRight (* 4 (+ weight 1)))
            (.setScale scale RoundingMode/DOWN))))))
