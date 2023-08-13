(ns pg.out
  (:require
   [pg.bytes :as bytes])
  (:import
   java.util.List
   java.io.ByteArrayOutputStream))


(defn create []
  (new ByteArrayOutputStream))


(defn write-cstring
  [^ByteArrayOutputStream out
   ^String string
   ^String encoding]
  (doto out
    (.writeBytes (.getBytes string encoding))
    (.write 0)))


(defn write-byte
  [^ByteArrayOutputStream out
   ^Integer value]
  (doto out
    (.write value)))


(defn write-bytes
  [^ByteArrayOutputStream out
   ^bytes value]
  (doto out
    (.writeBytes value)))


(defn write-char
  [^ByteArrayOutputStream out
   ^Character value]
  (doto out
    (.write (byte value))))


(defn write-int32
  [^ByteArrayOutputStream out value]
  (doto out
    (.writeBytes (bytes/int32->bytes value))))


(defn write-int16
  [^ByteArrayOutputStream out value]
  (doto out
    (.writeBytes (bytes/int16->bytes value))))


(defn write-uint16
  [^ByteArrayOutputStream out value]
  (doto out
    (.writeBytes (bytes/uint16->bytes value))))


(defn write-uint32
  [^ByteArrayOutputStream out value]
  (doto out
    (.writeBytes (bytes/uint32->bytes value))))


(defn write-int16s
  [^ByteArrayOutputStream out
   ^List values]
  (let [len (count values)]
    (loop [i 0]
      (when (< i len)
        (let [value (.get values i)]
          (.writeBytes out (bytes/int16->bytes value))
          (recur (inc i))))))
  out)


(defn array ^bytes [^ByteArrayOutputStream out]
  (.toByteArray out))
