(ns pg.encode.bin.numeric
  (:require
   [clojure.string :as str]
   [pg.bb :as bb]
   [pg.bytes :as bytes]
   [pg.const :as const]
   [pg.encode.bin.core :refer [expand
                               -encode]]
   [pg.oid :as oid])
  (:import
   clojure.lang.BigInt
   java.math.BigDecimal
   java.math.BigInteger
   java.math.RoundingMode
   java.util.ArrayList
   java.util.List))


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
        (if negative?
          const/NUMERIC_NEG
          const/NUMERIC_POS)

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


;;
;; BigDecimal
;;

(expand [BigDecimal nil
         BigDecimal oid/numeric]
  [value _ opt]
  (BigDecimal-numeric value opt))


(expand [BigDecimal oid/int2]
  [^BigDecimal value _ opt]
  (-> value
      (.shortValueExact)
      (bytes/int16->bytes)))


(expand [BigDecimal oid/int4]
  [^BigDecimal value _ opt]
  (-> value
      (.intValueExact)
      (bytes/int32->bytes)))


(expand [BigDecimal oid/int8]
  [^BigDecimal value _ opt]
  (-> value
      (.longValueExact)
      (bytes/int64->bytes)))


(expand [BigDecimal oid/float4]
  [^BigDecimal value _ opt]
  (-> value
      (.floatValue)
      (Float/floatToIntBits)
      (bytes/int32->bytes)))


(expand [BigDecimal oid/float8]
  [^BigDecimal value _ opt]
  (-> value
      (.doubleValue)
      (Double/doubleToLongBits)
      (bytes/int64->bytes)))


;;
;; BigInt
;;

(expand [BigInt nil
         BigInt oid/numeric]
  [value _ opt]
  (BigInt-numeric value opt))


(expand [BigInt oid/int2]
  [value _ opt]
  (bytes/int16->bytes (short value)))


(expand [BigInt oid/int4]
  [value _ opt]
  (bytes/int32->bytes (int value)))


(expand [BigInt oid/int8]
  [value _ opt]
  (bytes/int64->bytes (long value)))


;;
;; BigInteger
;;

(expand [BigInteger nil
         BigInteger oid/numeric
         BigInteger oid/int2
         BigInteger oid/int4
         BigInteger oid/int8]
  [^BigInteger value oid opt]
  (-encode (new BigDecimal value)
           oid
           opt))
