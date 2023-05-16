(ns pg.client.connection
  (:import
   pg.client.message.ReadyForQuery
   pg.client.message.ErrorResponse
   pg.client.message.AuthenticationOk)
  (:require
   [pg.client.message :as message]
   [pg.error :as e]
   [pg.client.parse :as parse]
   [pg.client.handle :as handle]
   [pg.client.result :as result]
   [pg.client.compose :as compose]
   [pg.client.bb :as bb])
  (:import
   java.nio.ByteBuffer
   java.io.Closeable
   java.util.Map
   java.util.HashMap
   java.net.InetSocketAddress
   java.nio.channels.SocketChannel))


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


(defprotocol IConnection

  (set-pid [this pid])

  (get-pid [this])

  (set-secret-key [this secret-key])

  (get-secret-key [this])

  (set-tx-status [this tx-status])

  (get-tx-status [this])

  (set-parameter [this param value])

  (get-server-encoding [this])

  (get-client-encoding [this])

  (get-parameter [this param])

  (read-message [this])

  (read-messages [this])

  (read-messages-until [this set-classes])

  (send-message [this bb])

  (authenticate [this])

  (initiate [this])

  (query [this str-sql]))


(deftype Connection
    [^Map -config
     ^InetSocketAddress -addr
     ^SocketChannel -ch
     ^Map -params
     ^Map -state]

  IConnection

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
            (bb/allocate len)]

        (read-bb -ch bb-body)
        (bb/rewind bb-body)

        (parse/-parse tag bb-body))))

  (read-messages [this]
    (lazy-seq (cons (read-message this)
                    (read-messages this))))

  (read-messages-until [this set-classes]
    (let [pred
          (fn [msg]
            (contains? set-classes (type msg)))]
      (take-until pred (read-messages this))))

  (authenticate [this]

    (let [{:keys [database user]}
          -config

          bb
          (compose/startup database user)

          result
          (result/result this)

          messages
          (read-messages-until this #{AuthenticationOk ErrorResponse})]

      (send-message this bb)

      (handle/handle result messages)))

  (initiate [this]

    (let [messages
          (read-messages-until this #{ReadyForQuery})

          result
          (result/result this)]

      (handle/handle result messages)))

  (query [this str-sql]

    (let [bb
          (compose/query str-sql)

          messages
          (read-messages-until this #{ReadyForQuery})

          result
          (result/result this)]

      (send-message this bb)

      (-> result
          (handle/handle messages)
          (result/complete))))

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
                    :user "ivan"
                    :password "ivan"
                    :database "ivan"}))

  (authenticate -c)

  (initiate -c)

  (def -r (query -c "select 1 as foo; select 2 as bar"))

  )
