(ns pg.bytes.array)


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


(defn arr64 ^bytes [value]
  (let [buf
        (-> value
            (BigInteger/valueOf)
            (.toByteArray))

        pad
        (- 8 (alength buf))]

    (if (pos? pad)
      (byte-array (-> []
                      (into (repeat pad 0))
                      (into buf)))
      buf)))
