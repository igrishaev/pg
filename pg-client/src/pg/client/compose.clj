(ns pg.client.compose
  (:require
   [pg.client.codec :as codec]
   [pg.client.bb :as bb]))


(defn startup

  ([^String database ^String user]
   (startup database user 196608))

  ([^String database ^String user ^Integer protocol-version]
   (let [len (+ 4 4 4
                (codec/bytes-count user) 1
                1 8 1
                (codec/bytes-count database) 1
                1)]

     (doto (bb/allocate len)
       (bb/write-int32 len)
       (bb/write-int32 protocol-version)
       (bb/write-cstring (codec/str->bytes "user"))
       (bb/write-cstring (codec/str->bytes user))
       (bb/write-cstring (codec/str->bytes "database"))
       (bb/write-cstring (codec/str->bytes database))
       (bb/write-byte 0)))))


(defn query [^String query]
  (let [len (+ 4 (codec/bytes-count query) 1)]
    (doto (bb/allocate (inc len))
      (bb/write-byte \Q)
      (bb/write-int32 len)
      (bb/write-cstring (codec/str->bytes query)))))
