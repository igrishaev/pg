(ns pg.bb
  (:import
   java.nio.ByteBuffer))


(defn allocate ^ByteBuffer [size]
  (ByteBuffer/allocate size))


(defn wrap ^ByteBuffer [^bytes buf]
  (ByteBuffer/wrap buf))


(defn remaining [^ByteBuffer bb]
  (.remaining bb))


(defn read-int16 [^ByteBuffer bb]
  (.getShort bb))


(defmacro rewind [bb]
  `(.. ~(with-meta bb {:tag `ByteBuffer}) (rewind)))


(defmacro read-int32 [^ByteBuffer bb]
  `(.. ~(with-meta bb {:tag `ByteBuffer}) (getInt)))


(defn read-int32s [^ByteBuffer bb amount]
  (loop [i 0
         acc (transient [])]
    (if (= i amount)
      (persistent! acc)
      (recur (inc i)
             (conj! acc (read-int32 bb))))))


(defn read-rest ^bytes [^ByteBuffer bb]
  (let [buf (byte-array (.remaining bb))]
    (.get bb buf)
    buf))


(defmacro array [bb]
  `(.. ~(with-meta bb {:tag `ByteBuffer}) (array)))


(defn read-byte [^ByteBuffer bb]
  (.get bb))


(defn read-bytes ^bytes [^ByteBuffer bb amount]
  (let [buf (byte-array amount)]
    (.get bb buf)
    buf))


(defn read-cstring ^bytes [^ByteBuffer bb]

  (let [pos
        (.position bb)

        offset
        (loop [offset 0]
           (if (zero? (.get bb (+ pos offset)))
             offset
             (recur (inc offset))))

        buf
        (byte-array offset)]

    (.get bb buf)
    buf))


(defn read-cstrings [^ByteBuffer bb amount]
  (loop [i 0
         acc (transient [])]
    (if (= i amount)
      (persistent! acc)
      (recur (inc i)
             (conj! acc (read-cstring bb))))))


(defn write-int32 [^ByteBuffer bb value]
  (.putInt bb value))


(defn write-int32s [^ByteBuffer bb int32s]
  (loop [[int32 & int32s] int32s]
    (if int32
      (do
        (write-int32 bb int32)
        (recur int32s))
      bb)))


(defn write-int16 [^ByteBuffer bb value]
  (.putShort bb value))


(defn write-int16s [^ByteBuffer bb int16s]
  (loop [[int16 & int16s] int16s]
    (if int16
      (do
        (write-int16 bb int16)
        (recur int16s))
      bb)))


(defn write-byte [^ByteBuffer bb value]
  (.put bb (byte value)))


(defn write-bytes [^ByteBuffer bb ^bytes  buf]
  (.put bb buf))


(defn write-cstring
  [^ByteBuffer bb ^bytes string]
  (doto bb
    (write-bytes string)
    (write-byte 0)))
