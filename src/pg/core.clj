(ns pg.core
  (:require
   [clojure.java.io :as io])
  (:import
   java.util.Scanner
   java.nio.channels.SocketChannel
   java.nio.ByteBuffer
   java.io.BufferedReader
   java.io.ByteArrayOutputStream
   java.io.InputStream
   java.io.OutputStream
   java.io.InputStreamReader
   java.net.InetSocketAddress
   java.net.Socket)
  (:gen-class))


(defn read-int32 [^InputStream in]
  (let [buf (byte-array 4)]
    (.read in buf)
    (.intValueExact (new BigInteger buf))))


(defn read-int16 [^InputStream in]
  (let [buf (byte-array 2)]
    (.read in buf)
    (.intValueExact (new BigInteger buf))))


(defn read-cstring [^InputStream in]

  (let [o (new ByteArrayOutputStream)]
    (loop []
      (let [b (.read in)]
        (cond
          (= -1 b)
          nil

          (= 0 b)
          (.toString o "UTF-8")

          :else
          (do
            (.write o b)
            (recur))))))

  #_
  (let [s (new Scanner in "UTF-8")]
    (.useDelimiter s "\0")
    (when (.hasNext s)
      (.next s)))

  #_
  (with-open [r (new InputStreamReader in "UTF-8")]
    (with-open [b (new BufferedReader r 1)]
      (loop [sb (new StringBuilder)]
        (let [c (.read b)]
          (cond
            (= -1 c)
            :EOF

            (= 0 c)
            (str sb)

            :else
            (recur (.append sb (char c)))))))))



(defmulti mm-read-message
  (fn [b in]
    b))


(defmethod mm-read-message (byte \S)
  [_ in]
  (let [len (read-int32 in)]
    len
    )
  )

(defmethod mm-read-message (byte \T) ;; RowDescription
  [_ in]
  (let [len
        (read-int32 in)

        field-count
        (read-int16 in)

        fields
        (doall
         (for [_ (range field-count)]
           {:name (read-cstring in)
            :table-id (read-int32 in)
            :column-id (read-int16 in)
            :type-id (read-int32 in)
            :type-size (read-int16 in)
            :type-mod-id (read-int32 in)
            :format (read-int16 in)}))]

    fields))


(defn bb>int16 [^ByteBuffer bb]
  (.getShort bb))


(defn bb>int32 [^ByteBuffer bb]
  (.getInt bb))


(defn bb>byte [^ByteBuffer bb]
  (.get bb))


(defn bb>bytes [^ByteBuffer bb n]
  ())


(defn bb>cstring [^ByteBuffer bb ^String encoding]

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

    string))





(defn send-message [^OutputStream out & values]

  )


(defn write-byte [^OutputStream out b]
  (.write out (byte b)))


(defn write-int32 [^OutputStream out int32]
  (let [buf (int32->bytes int32)]
    (.write out ^bytes buf)))


(defn write-cstring

  ([^OutputStream out ^String string]
   (write-cstring out string "UTF-8"))

  ([^OutputStream out ^String string ^String encoding]
   (let [buf (.getBytes string encoding)]
     (.write out ^bytes buf)
     (.write out (byte 0)))))


(defn send-startup [^OutputStream out user database]
  (let [len (+ 4 4 4 1 (count user) 1 8 1 (count database) 1 1)]
    (doto out
      (write-int32 len)
      (write-int32 196608)
      (write-cstring "user")
      (write-cstring user)
      (write-cstring "database")
      (write-cstring database)
      (write-byte 0))))


(defn send-query [^OutputStream out query]
  (let [len (+ 4 (count query) 1)]
    (doto out
      (write-byte \Q)
      (write-int32 len)
      (write-cstring query))))


(defn send-flush [^OutputStream out]
  (doto out
    (write-byte \H)
    (write-int32 4)))


(defn send-sync [^OutputStream out]
  (doto out
    (write-byte \S)
    (write-int32 4)))


(defn send-term [^OutputStream out]
  (doto out
    (write-byte \X)
    (write-int32 4)))


(defn bytes->int32 [^bytes buf]
  (let [b4 (aget buf 0)
        b3 (aget buf 1)
        b2 (aget buf 2)
        b1 (aget buf 3)]
    (+ (bit-shift-left b4 24)
       (bit-shift-left b3 16)
       (bit-shift-left b2 8)
       b1)))


(defn int32->bytes ^bytes [int32]
  (let [buf (byte-array 4)

        b4 (-> int32 (bit-and 0xff000000) (bit-shift-right 24) unchecked-byte)
        b3 (-> int32 (bit-and 0x00ff0000) (bit-shift-right 16) unchecked-byte)
        b2 (-> int32 (bit-and 0x0000ff00) (bit-shift-right 8) unchecked-byte)
        b1 (-> int32 (bit-and 0x000000ff) unchecked-byte)]

    (aset buf 0 b4)
    (aset buf 1 b3)
    (aset buf 2 b2)
    (aset buf 3 b1)

    buf))


(defn read-message [^InputStream in]
  (let [b (.read in)]
    (when-not (= b -1)
      (mm-read-message (byte b) in))))


(defn read-bytes [^InputStream in n]
  (let [buf (byte-array n)]
    (.read in buf)
    buf))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))


(comment

  (def ^Socket -s (new Socket "localhost" 15432))

  (def -in (.getInputStream -s))
  (def -out (.getOutputStream -s))



  (def -b (byte-array 32))
  (.read -in -b)



  (.write -out (byte-array [0, 0, 0, 33, 0, 3, 0, 0, 117, 115, 101, 114, 0, 105, 118, 97, 110, 0, 100, 97,
                            116, 97, 98, 97, 115, 101, 0, 105, 118, 97, 110, 0, 0]))

  ;; user
  ;; database


  (def -len (+ 4 4 4 1 4 1 8 1 4 1 1))

  (def ^SocketChannel
    -c (SocketChannel/open))

  (def ^InetSocketAddress
    -addr (new InetSocketAddress "127.0.0.1" 15432))

  (.connect -c -addr)

  (def ^ByteBuffer
    -bb (ByteBuffer/allocate -len))

  (doto -bb
    (.putInt -len)
    (.putInt 196608)

    (.put (.getBytes "user"))
    (.put (byte 0))

    (.put (.getBytes "ivan"))
    (.put (byte 0))

    (.put (.getBytes "database"))
    (.put (byte 0))

    (.put (.getBytes "ivan"))
    (.put (byte 0))

    (.put (byte 0)))

  #_
  (.flip -bb)

  (.rewind -bb)

  (.write -c -bb)

  (def ^ByteBuffer
    -bb2 (ByteBuffer/allocate 32))

  (.read -c -bb2)


  (.rewind -bb2)
  (.array -bb2)

  (.get -bb2)

  (.getInt -bb2)


  )
