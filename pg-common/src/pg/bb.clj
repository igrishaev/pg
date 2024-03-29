(ns pg.bb
  (:require
   [pg.bytes :as bytes])
  (:import
   java.io.ByteArrayOutputStream
   java.nio.ByteBuffer))


(defmacro allocate [size]
  `(ByteBuffer/allocate ~size))


(defmacro wrap ^ByteBuffer [^bytes buf]
  `(ByteBuffer/wrap ~buf))


(defn skip ^ByteBuffer[^ByteBuffer bb offset]
  (let [pos ^Integer (+ (.position bb) offset)]
    (doto bb
      (.position pos))))


(defmacro remaining [bb]
  `(.. ~(with-meta bb {:tag `ByteBuffer}) (remaining)))


(defmacro rewind [bb]
  `(.. ~(with-meta bb {:tag `ByteBuffer}) (rewind)))


(defn write-int16 [^ByteBuffer bb value]
  (.putShort bb value))


(defn write-uint16 [^ByteBuffer bb value]
  (.put bb (bytes/uint16->bytes value)))


(defn write-uint32 [^ByteBuffer bb value]
  (.put bb (bytes/uint32->bytes value)))


(defn write-int32 [^ByteBuffer bb value]
  (.putInt bb value))


(defn write-int64 [^ByteBuffer bb value]
  (.putLong bb value))


(defn write-byte [^ByteBuffer bb value]
  (.put bb (byte value)))


(defn write-bytes [^ByteBuffer bb ^bytes buf]
  (.put bb buf))


(defn write-cstring
  [^ByteBuffer bb ^String string ^String encoding]
  (doto bb
    (write-bytes (.getBytes string encoding))
    (write-byte 0)))


(defn read-byte [^ByteBuffer bb]
  (.get bb))


(defn read-bytes ^bytes [^ByteBuffer bb len]
  (let [buf (byte-array len)]
    (.get bb buf)
    buf))


(defn read-cstring ^String [^ByteBuffer bb ^String encoding]
  (let [out (new ByteArrayOutputStream)]
    (loop []
      (let [b (.get bb)]
        (if (zero? b)
          (.toString out encoding)
          (do
            (.write out b)
            (recur)))))))


(defmacro read-int16 [^ByteBuffer bb]
  `(.. ~(with-meta bb {:tag `ByteBuffer}) (getShort)))


(defn read-uint16 [^ByteBuffer bb]
  (-> bb (.getShort) (Short/toUnsignedInt)))


(defmacro read-int32 [^ByteBuffer bb]
  `(.. ~(with-meta bb {:tag `ByteBuffer}) (getInt)))


(defmacro read-int64 [^ByteBuffer bb]
  `(.. ~(with-meta bb {:tag `ByteBuffer}) (getLong)))


(defn read-rest ^bytes [^ByteBuffer bb]
  (let [buf (byte-array (.remaining bb))]
    (.get bb buf)
    buf))


(defn array ^bytes [^ByteBuffer bb]
  (.array bb))


(defn to-vector [^ByteBuffer bb]
  (vec (.array bb)))
