(ns pg.client2.out
  (:require
   [pg.client2.bytes :as bytes])
  (:import java.io.ByteArrayOutputStream))


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
  [^ByteArrayOutputStream out
   ^Integer value]
  (doto out
    (.writeBytes (bytes/int32->bytes value))))


(defn write-int16
  [^ByteArrayOutputStream out
   ^Integer value]
  (doto out
    (.writeBytes (bytes/int16->bytes value))))


(defn array ^bytes [^ByteArrayOutputStream out]
  (.toByteArray out))
