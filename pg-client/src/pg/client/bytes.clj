(ns pg.client.bytes
  (:refer-clojure :exclude [concat empty? ==])
  (:import
   java.util.Arrays))


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
