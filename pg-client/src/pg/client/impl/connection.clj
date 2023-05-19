(ns pg.client.impl.connection
  (:require
   [pg.client.bb :as bb]
   [pg.client.impl.result :as result]
   [pg.client.prot.result :as prot.result]
   [pg.client.prot.message :as message]
   pg.client.impl.message
   [pg.client.prot.connection :as connection]
   [pg.error :as e])
  (:import
   java.io.Closeable
   java.net.InetSocketAddress
   java.nio.ByteBuffer
   java.nio.channels.SocketChannel
   java.util.HashMap
   java.util.Map
   pg.client.impl.message.Query
   pg.client.impl.message.StartupMessage
   pg.client.impl.message.AuthenticationOk
   pg.client.impl.message.ErrorResponse
   pg.client.impl.message.ReadyForQuery))


(defn read-bb [^SocketChannel ch ^ByteBuffer bb]
  (while (not (zero? (bb/remaining bb)))
    (.read ch bb)))


(defn take-until
  "Returns a lazy sequence of successive items from coll until
  (pred item) returns true, including that item. pred must be
  free of side-effects. Returns a transducer when no collection
  is provided."
  {:added "1.7"
   :static true}
  ([pred]
   (fn [rf]
     (fn
       ([] (rf))
       ([result] (rf result))
       ([result input]
        (if (pred input)
          (ensure-reduced (rf result input))
          (rf result input))))))
  ([pred coll]
   (lazy-seq
    (when-let [s (seq coll)]
      (if (pred (first s))
        (cons (first s) nil)
        (cons (first s) (take-until pred (rest s))))))))


(defn byte? [x]
  (instance? Byte x))


(deftype Connection
    [^Map -config
     ^InetSocketAddress -addr
     ^SocketChannel -ch
     ^Map -params
     ^Map -state]

    connection/IConnection

    (set-pid [this pid]
      (.put -state "pid" pid))

    (get-pid [this]
      (.get -state "pid"))

    (set-secret-key [this secret-key]
      (.put -state "secret-key" secret-key))

    (get-secret-key [this]
      (.get -state "secret-key"))

    (set-tx-status [this tx-status]
      (.put -state "tx-status" tx-status))

    (get-tx-status [this]
      (.get -state "tx-status"))

    (set-parameter [this param value]
      (.put -params param value))

    (get-parameter [this param]
      (.get -params param))

    (get-password [this]
      (:password -config))

    (get-user [this]
      (:user -config))

    (get-server-encoding [this]
      (or (.get -params "server_encoding") "UTF-8"))

    (get-client-encoding [this]
      (or (.get -params "client_encoding") "UTF-8"))

    (send-message [this bb]

      (let [written (.write -ch (bb/rewind bb))
            remaining (bb/remaining bb)]
        (when-not (zero? remaining)
          (e/error! "Incomplete record to the channel, written: %s, remaining: %s"
                    written remaining))))

    (read-message [this]

      (let [bb-head
            (bb/allocate 5)]

        (read-bb -ch bb-head)

        (bb/rewind bb-head)

        (let [tag
              (char (bb/read-byte bb-head))

              len
              (- (bb/read-int32 bb-head) 4)

              bb-body
              (bb/allocate len)

              message-empty
              (message/tag->message tag)]

          (message/from-bb message-empty bb-body this))))

    (read-messages [this]
      (lazy-seq (cons (connection/read-message this)
                      (connection/read-messages this))))

    (read-messages-until [this set-classes]
      (let [pred
            (fn [msg]
              (contains? set-classes (type msg)))]
        (take-until pred (connection/read-messages this))))

    (authenticate [this]

      (let [{:keys [database user]}
            -config

            message
            (new StartupMessage 196608 user database nil)

            bb
            (message/to-bb message this)

            result
            (result/result this)

            _ (connection/send-message this bb)

            messages
            (connection/read-messages-until this #{AuthenticationOk ErrorResponse})]

        (prot.result/handle result messages)))

    (initiate [this]

      (let [messages
            (connection/read-messages-until this #{ReadyForQuery})

            result
            (result/result this)]

        (prot.result/handle result messages)))

    (write-message [this items]

      (let [[tag parts]
            items

            encoding
            (connection/get-client-encoding this)

            len-payload
            (reduce
             (fn [result part]
               (cond

                 (byte? part)
                 (inc result)

                 (bytes? part)
                 (+ result (alength ^bytes part) part)

                 (string? part)
                 (+ result (alength (.getBytes ^String part encoding)) 1)))
             0
             parts)

            len-header
            (if tag 5 4)

            bb
            (bb/allocate (+ len-header len-payload))]

        (when tag
          (bb/write-byte bb tag))

        (bb/write-int32 bb len-payload)

        (doseq [part parts]
          (cond

            (byte? part)
            (bb/write-byte part)

            (bytes? part)
            (bb/write-bytes bb part)

            (string? part)
            (bb/write-cstring bb part encoding)))

        (bb/rewind bb)

        (let [written (.write -ch bb)
              remaining (bb/remaining bb)]
          (when-not (zero? remaining)
            (e/error! "Incomplete record to the channel, written: %s, remaining: %s"
                      written remaining)))))

    (query [this sql]

      (let [bb
            (doto (new Query sql)
              (message/to-bb this))

            messages
            (connection/read-messages-until this #{ReadyForQuery})

            result
            (result/result this)]

        (connection/send-message this bb)
        (prot.result/handle result messages)))

    Closeable

    (close [this]
      (.close -ch)))


(defn connect [{:as config
                :keys [^String host
                       ^Integer port]}]

  (let [addr
        (new java.net.InetSocketAddress host port)

        ch
        (SocketChannel/open addr)

        params
        (new HashMap)]

    (new Connection
         config
         addr
         ch
         (new HashMap)
         (new HashMap))))


#_
(comment

  (def -c (connect {:host "127.0.0.1"
                    :port 15432
                    :user "foobar"
                    :password "foobar"
                    :database "ivan"}))

  (connection/authenticate -c)

  (connection/initiate -c)

  (def -r (connection/query -c "select 1 as foo; select 2 as bar"))

  )
