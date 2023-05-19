(ns pg.client.compose
  (:import
   java.nio.ByteBuffer)
  (:require
   [pg.client.codec :as codec]
   [pg.client.bb :as bb]))


(defn startup

  (^ByteBuffer [^String database ^String user]
   (startup database user 196608))

  (^ByteBuffer [^String database ^String user ^Integer protocol-version]
   (let [len (+ 4 4 4
                (codec/bytes-count user "UTF-8") 1
                1 8 1
                (codec/bytes-count database "UTF-8") 1
                1)]

     (doto (bb/allocate len)
       (bb/write-int32 len)
       (bb/write-int32 protocol-version)
       (bb/write-cstring "user" "UTF-8")
       (bb/write-cstring user "UTF-8")
       (bb/write-cstring "database" "UTF-8")
       (bb/write-cstring database "UTF-8")
       (bb/write-byte 0)))))


(defn query ^ByteBuffer [^String query]
  (let [len (+ 4 (codec/bytes-count query) 1)]
    (doto (bb/allocate (inc len))
      (bb/write-byte \Q)
      (bb/write-int32 len)
      (bb/write-cstring (codec/str->bytes query)))))


(defn query2 [^String query]
  [\Q query])


(defn startup2

  (^ByteBuffer [^String database ^String user]
   (startup database user 196608))

  (^ByteBuffer [^String database ^String user ^Integer protocol-version]

   [nil
    protocol-version
    "user"
    user
    "database"
    database
    (byte 0)]))
