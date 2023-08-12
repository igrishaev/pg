(ns pg.bytes
  (:refer-clojure :exclude [concat empty? ==])
  (:import
   java.util.Arrays
   java.nio.ByteBuffer))


(defn bytes->int16 ^Short [^bytes buf]
  (-> (ByteBuffer/wrap buf)
      (.getShort)))


(defn bytes->int32 ^Integer [^bytes buf]
  (-> (ByteBuffer/wrap buf)
      (.getInt)))


(defn bytes->int64 ^Long [^bytes buf]
  (-> (ByteBuffer/wrap buf)
      (.getLong)))


(defn bytes->float4 ^Float [^bytes buf]
  (-> (ByteBuffer/wrap buf)
      (.getFloat)))


(defn bytes->float8 ^Double [^bytes buf]
  (-> (ByteBuffer/wrap buf)
      (.getDouble)))


(defn int16->bytes ^bytes [^Short value]
  (-> (ByteBuffer/allocate 2)
      (.putShort value)
      (.array)))


(defn uint16->bytes ^bytes [value]
  (when-not (<= 0 value 0xFFFF)
    (throw
     (ex-info
      (format "uint16->bytes error: value %s is out of range" value)
      {:value value})))
  (let [b1 (-> value (bit-and 0xFF00) (bit-shift-right 8))
        b2 (-> value (bit-and 0xFF))]
    (byte-array [b1 b2])))


(defn int32->bytes ^bytes [^Integer value]
  (-> (ByteBuffer/allocate 4)
      (.putInt value)
      (.array)))


(defn int64->bytes ^bytes [^Long value]
  (-> (ByteBuffer/allocate 8)
      (.putLong value)
      (.array)))


(defn concat ^bytes [^bytes bytes1 ^bytes bytes2]
  (let [result
        (byte-array (+ (alength bytes1) (alength bytes2)))]
    (System/arraycopy bytes1 0 result 0                (alength bytes1))
    (System/arraycopy bytes2 0 result (alength bytes1) (alength bytes2))
    result))


(defn == ^Boolean [^bytes bytes1 ^bytes bytes2]
  (Arrays/equals bytes1 bytes2))


(defn empty? ^Boolean [^bytes bytes]
  (zero? (alength bytes)))


(defn xor
  ^bytes [^bytes bytes1 ^bytes bytes2]

  (when-not (= (alength bytes1) (alength bytes2))
    (throw (new Exception "XOR error: the lengths do not match")))

  (let [len
        (alength bytes1)]

    (loop [result (byte-array len)
           i 0]

      (if (= i len)
        result

        (let [b1 (aget bytes1 i)
              b2 (aget bytes2 i)]
          (aset result i ^Byte (bit-xor b1 b2))
          (recur result (inc i)))))))


(defmacro byte? [x]
  `(instance? Byte ~x))
