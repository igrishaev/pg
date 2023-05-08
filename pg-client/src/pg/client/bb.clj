(ns pg.client.bb
  (:import
   java.io.ByteArrayOutputStream
   java.nio.ByteBuffer))


(defmacro remaining [bb]
  `(.. ~(with-meta bb {:tag `ByteBuffer}) (remaining)))


(defmacro rewind [bb]
  `(.. ~(with-meta bb {:tag `ByteBuffer}) (rewind)))


(defn read-int16 [^ByteBuffer bb]
  (.getShort bb))


(defmacro allocate [size]
  `(ByteBuffer/allocate ~size))


(defn write-int32 [^ByteBuffer bb value]
  (.putInt bb value))


(defn write-byte [^ByteBuffer bb value]
  (.put bb (byte value)))


(defn write-bytes [^ByteBuffer bb ^bytes  buf]
  (.put bb buf))


(defn write-cstring
  [^ByteBuffer bb ^bytes string]
  (doto bb
    (write-bytes string)
    (write-byte 0)))


#_
(defn write-cstring
  ([^ByteBuffer bb ^String string]
   (write-cstring bb string "UTF-8"))

  ([^ByteBuffer bb ^String string ^String encoding]
   (doto bb
     (write-bytes (.getBytes string encoding))
     (write-byte 0))))


(defn read-byte [^ByteBuffer bb]
  (.get bb))


(defn read-bytes ^bytes [^ByteBuffer bb len]
  (let [buf (byte-array len)]
    (.get bb buf)
    buf))


#_
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
    (.get bb)
    buf))


(defn read-cstring

  (^String [^ByteBuffer bb]
   (read-cstring bb "UTF-8"))

  (^String [^ByteBuffer bb ^String encoding]
   (let [out (new ByteArrayOutputStream)]
     (loop []
       (let [b (.get bb)]
         (if (zero? b)
           (.toString out encoding)
           (do
             (.write out b)
             (recur))))))))


(defmacro read-int32 [^ByteBuffer bb]
  `(.. ~(with-meta bb {:tag `ByteBuffer}) (getInt)))
