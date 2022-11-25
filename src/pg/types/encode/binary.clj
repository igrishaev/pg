(ns pg.types.encode.binary
  (:import
   java.util.UUID
   clojure.lang.Symbol)
  (:require
   [pg.oid :as oid]
   [pg.bb :as bb]
   [pg.bytes :as b]
   [pg.error :as e]
   [pg.codec :as codec]))


(defmacro get-enc [options]
  `(get ~options :encoding "UTF-8"))


(defmulti mm-encode
  (fn [value oid _]
    [(class value) oid]))


(defmethod mm-encode :default
  [value oid opt]
  (e/error! (format "Cannot binary encode value: [%s %s]"
                    (type value) oid)
            {:oid oid
             :opt opt
             :value value
             :class (type value)}))


(defmethod mm-encode [String oid/TEXT]
  [value _ opt]
  (codec/str->bytes value (get-enc opt)))


(defmethod mm-encode [Symbol oid/TEXT]
  [value _ opt]
  (codec/str->bytes (str value) (get-enc opt)))


(defmethod mm-encode [Character oid/CHAR]
  [value _ opt]
  (codec/str->bytes (str value) (get-enc opt)))


(defmethod mm-encode [Boolean oid/BOOL]
  [value _ _]
  (let [b
        (case value
          true 1
          false 0)]
    (byte-array [b])))


(defn int->bytes ^bytes [value len]
  (byte-array
   (loop [i 0
          acc []]
     (if (= i len)
       acc
       (let [b
             (-> value
                 (bit-shift-right (* 8 (- len i 1)))
                 (bit-and 0xff)
                 unchecked-byte)]
         (recur (inc i)
                (conj acc b)))))))


(defmethod mm-encode [Integer oid/INT2]
  [^Integer value _ _]
  (int->bytes (short value) 2))


(defmethod mm-encode [Integer oid/INT4]
  [^Integer value _ _]
  (int->bytes value 4))


(defmethod mm-encode [Integer oid/INT8]
  [^Integer value _ _]
  (int->bytes value 8))


(defmethod mm-encode [Long oid/INT2]
  [value oid opt]
  (mm-encode (short value) oid opt))


(defmethod mm-encode [Long oid/INT4]
  [value oid opt]
  (mm-encode (int value) oid opt))


(defmethod mm-encode [Long oid/INT8]
  [value _ _]
  (int->bytes value 8))


(defmethod mm-encode [Float oid/FLOAT4]
  [^Float value _ _]
  (-> (Float/floatToIntBits value)
      (int->bytes 4)))


(defmethod mm-encode [Double oid/FLOAT4]
  [^Double value oid opt]
  (mm-encode (float value) oid opt))


(defmethod mm-encode [Double oid/FLOAT8]
  [^Double value _ _]
  (-> (Double/doubleToLongBits value)
      (int->bytes 8)))


#_
java.math.BigDecimal

#_
(defmethod mm-encode [BigInteger oid/INT8]
  [^BigInteger value _ _]
  (int->bytes value 8))


(defmethod mm-encode [UUID oid/UUID]
  [^UUID value _ _]
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


(defmethod mm-encode [UUID oid/TEXT]
  [^UUID value oid opt]
  (mm-encode (str value) oid opt))
