(ns pg.client2.bytes
  (:import
   java.nio.ByteBuffer))


(defn int16->bytes ^bytes [^Short value]
  (-> (ByteBuffer/allocate 2)
      (.putShort value)
      (.array)))


(defn int32->bytes ^bytes [^Integer value]
  (-> (ByteBuffer/allocate 4)
      (.putInt value)
      (.array)))


(defn int64->bytes ^bytes [^Long value]
  (-> (ByteBuffer/allocate 8)
      (.putLong value)
      (.array)))
