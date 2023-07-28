(ns pg.encode.bin.numeric
  (:require
   [clojure.string :as str]
   [pg.bb :as bb]
   [pg.const :as const])
  (:import
   java.util.List
   java.util.ArrayList
   java.math.RoundingMode
   clojure.lang.BigInt
   java.math.BigInteger
   java.math.BigDecimal))


(defn parse-short [x]
  (Short/parseShort x))


(defn BigDecimal-numeric [^BigDecimal value opt]

  (let [scale
        (.scale value)

        [^String hi ^String lo]
        (-> value
            (.toPlainString)
            (str/split #"\."))

        negative?
        (-> hi (.charAt 0) (=  \-))

        hi
        (if negative?
          (subs hi 1)
          hi)

        sign
        (if negative? 1 0)

        weight
        (quot (count hi) 4)

        pad-left
        (- 4 (rem (count hi) 4))

        pad-right
        (- 4 (rem (+ pad-left (count hi) (count lo)) 4))

        digits-str
        (str (.repeat "0" pad-left) hi lo (.repeat "0" pad-right))

        digits-num
        (/ (count digits-str) 4)

        digits
        (loop [i 0
               acc []]
          (if (= i digits-num)
            acc
            (let [i-start (* i 4)
                  i-end (+ i-start 4)
                  part (subs digits-str i-start i-end)]
              (recur (inc i) (conj acc part)))))

        ^List digits-vec
        (mapv parse-short digits)

        bb-len
        (+ 8 (* 2 digits-num))

        bb
        (bb/allocate bb-len)]

    (bb/write-int16 bb digits-num)
    (bb/write-int16 bb weight)
    (bb/write-int16 bb sign)
    (bb/write-int16 bb scale)

    (loop [i 0]
      (when (< i digits-num)
        (let [digit (.get digits-vec i)]
          (bb/write-int16 bb digit)
          (recur (inc i)))))

    (bb/array bb)))


(defn BigInt-numeric [value opt]
  (-> value
      (bigdec)
      (BigDecimal-numeric opt)))
