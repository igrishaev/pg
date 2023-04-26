(ns pg.bytes.array
  (:import
   java.nio.ByteBuffer))


(defn arr16 ^bytes [^Short value]
  (-> (ByteBuffer/allocate 2)
      (.putShort value)
      (.array)))


(defn arr32 ^bytes [^Integer value]
  (-> (ByteBuffer/allocate 4)
      (.putInt value)
      (.array)))


(defn arr64 ^bytes [^Long value]
  (-> (ByteBuffer/allocate 8)
      (.putLong value)
      (.array)))
