(ns pg.bytes.array
  (:import
   java.nio.ByteBuffer))


;; TODO: bb

(defn arr16 ^bytes [value]
  (byte-array
   [(-> value (bit-and 0xff00) (bit-shift-right 8))
    (-> value (bit-and 0x00ff))]))


(defn arr32 ^bytes [value]
  (byte-array
   [(-> value (bit-and 0xff000000) (bit-shift-right 24))
    (-> value (bit-and 0x00ff0000) (bit-shift-right 16))
    (-> value (bit-and 0x0000ff00) (bit-shift-right  8))
    (-> value (bit-and 0x000000ff))]))


(defn arr64 ^bytes [^Long value]
  (-> (ByteBuffer/allocate 8)
      (.putLong value)
      (.array)))
