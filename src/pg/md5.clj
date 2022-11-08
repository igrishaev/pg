(ns pg.md5
  (:import
   java.security.MessageDigest))


(defn bytes->hex ^String [^bytes input]
  (let [sb (new StringBuilder)]
    (doseq [b input]
      (.append sb (format "%02x" b)))
    (str sb)))


(defn md5 ^bytes [^bytes input]
  (let [d (MessageDigest/getInstance "MD5")]
    (.update d input)
    (-> d
        (.digest)
        (bytes->hex)
        (.toLowerCase)
        (.getBytes))))
