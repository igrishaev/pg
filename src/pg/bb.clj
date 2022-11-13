(ns pg.bb
  (:import
   java.nio.ByteBuffer))


(defn allocate ^ByteBuffer [size]
  (ByteBuffer/allocate size))


(defn from-bytes ^ByteBuffer [^bytes buf]
  (ByteBuffer/wrap buf))


(defn read-int16 [^ByteBuffer bb]
  (.getShort bb))


(defmacro rewind [bb]
  `(.. ~(with-meta bb {:tag `ByteBuffer}) (rewind)))


(defmacro read-int32 [^ByteBuffer bb]
  `(.. ~(with-meta bb {:tag `ByteBuffer}) (getInt)))


(defn read-rest ^bytes [^ByteBuffer bb]
  (let [buf (byte-array (.remaining bb))]
    (.get bb buf)
    buf))


(defmacro get-array [bb]
  `(.. ~(with-meta bb {:tag `ByteBuffer}) (array)))


(defn read-byte [^ByteBuffer bb]
  (.get bb))


(defn read-bytes ^bytes [^ByteBuffer bb amount]
  (let [buf (byte-array amount)]
    (.get bb buf)
    buf))


(defn read-cstring

  ([^ByteBuffer bb]
   (read-cstring bb "UTF-8"))

  ([^ByteBuffer bb ^String encoding]

   (let [pos
         (.position bb)

         zero-pos
         (loop [i pos]
           (if (zero? (.get bb i))
             i
             (recur (inc i))))

         string
         (new String (.array bb) pos ^int (- zero-pos pos) encoding)]

     (.position bb ^int (inc zero-pos))

     string)))


(defn write-int32 [^ByteBuffer bb value]
  (.putInt bb value))


(defn write-int32s [^ByteBuffer bb int32s]
  (doseq [int32 int32s]
    (write-int32 bb int32))
  bb)


(defn write-int16 [^ByteBuffer bb value]
  (.putShort bb value))


(defn write-int16s [^ByteBuffer bb int16s]
  (doseq [int16 int16s]
    (write-int16 bb int16))
  bb)


(defn write-byte [^ByteBuffer bb value]
  (.put bb (byte value)))


(defn write-bytes [^ByteBuffer bb ^bytes  buf]
  (.put bb buf))


(defn write-cstring

  ([^ByteBuffer bb ^String string]
   (write-cstring bb string "UTF-8"))

  ([^ByteBuffer bb ^String string ^String encoding]
   (let [buf (.getBytes string encoding)]
     (.put bb buf)
     (.put bb (byte 0)))))
