(ns pg.types.encode.binary
  (:import
   java.util.UUID
   clojure.lang.Symbol)
  (:require
   [pg.bb :as bb]
   [pg.bytes :as b]
   [pg.error :as e]
   [pg.codec :as codec]))


(defmacro get-enc [options]
  `(get ~options :encoding "UTF-8"))


(defmulti mm-encode
  (fn [value _]
    (class value)))


(defmethod mm-encode :default
  [value _]
  (e/error! "Cannot binary encode value"
            {:value value}))


(defmethod mm-encode String
  [value opt]
  (codec/str->bytes value (get-enc opt)))


(defmethod mm-encode Symbol
  [value opt]
  (codec/str->bytes (str value) (get-enc opt)))


(defmethod mm-encode Character
  [value opt]
  (codec/str->bytes (str value) (get-enc opt)))


(defmethod mm-encode Boolean
  [value _]
  (let [b
        (case value
          true 1
          false 0)]
    (byte-array [b])))


(defmethod mm-encode Integer
  [^Integer value _]
  (-> (BigInteger/valueOf value)
      (.toByteArray)
      (b/zeros-left 4)))


(defmethod mm-encode Long
  [value _]
  (-> (BigInteger/valueOf value)
      (.toByteArray )
      (b/zeros-left 8)))


(defmethod mm-encode Float
  [^Float value _]
  (-> (Float/floatToIntBits value)
      (BigInteger/valueOf)
      (.toByteArray)
      (b/zeros-left 4)))


(defmethod mm-encode Double
  [^Double value _]
  (-> (Double/doubleToLongBits value)
      (BigInteger/valueOf)
      (.toByteArray)
      (b/zeros-left 8)))


(defmethod mm-encode BigInteger
  [^BigInteger value _]
  (-> value
      (.toByteArray)
      (b/zeros-left 8)))


(defmethod mm-encode UUID
  [^UUID value _]
  (let [bb
        (bb/allocate 16)

        most-bits
        (.getMostSignificantBits value)

        least-bits
        (.getLeastSignificantBits value)]

    (doto bb
      (bb/write-long8 most-bits)
      (bb/write-long8 least-bits))

    (bb/array bb)))
