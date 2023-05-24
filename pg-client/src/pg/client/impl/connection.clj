(ns pg.client.impl.connection
  (:require
   [pg.client.bb :as bb]
   [pg.client.coll :as coll]
   [pg.client.const :as const]
   [pg.client.debug :as debug]
   [pg.client.impl.message]
   [pg.client.impl.result :as result]
   [pg.client.prot.connection :as connection]
   [pg.client.prot.message :as message]
   [pg.client.prot.result :as prot.result])
  (:import
   java.io.Closeable
   java.net.InetSocketAddress
   java.nio.ByteBuffer
   java.nio.channels.SocketChannel
   java.util.HashMap
   java.util.Map
   pg.client.impl.message.AuthenticationOk
   pg.client.impl.message.ErrorResponse
   pg.client.impl.message.Query
   pg.client.impl.message.ReadyForQuery
   pg.client.impl.message.StartupMessage
   pg.client.impl.message.Terminate))


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

    (send-message [this message]
      (debug/debug-message message "<--")
      (let [bb (message/to-bb message this)]
        (bb/write-to -ch bb)))

    (read-message [this]

      (let [bb-head
            (bb/allocate 5)]

        (bb/read-from -ch bb-head)

        (bb/rewind bb-head)

        (let [tag
              (char (bb/read-byte bb-head))

              len
              (- (bb/read-int32 bb-head) 4)

              bb-body
              (bb/allocate len)

              _
              (bb/read-from -ch bb-body)

              message-empty
              (message/tag->message tag)

              _
              (bb/rewind bb-body)

              message
              (message/from-bb message-empty bb-body this)]

          (debug/debug-message message "-->")

          message)))

    (read-messages [this]
      (lazy-seq (cons (connection/read-message this)
                      (connection/read-messages this))))

    (read-messages-until [this set-classes]
      (let [pred
            (fn [msg]
              (contains? set-classes (type msg)))]
        (coll/take-until pred (connection/read-messages this))))

    (authenticate [this]

      (let [{:keys [database user]}
            -config

            message
            (new StartupMessage
                 const/PROTOCOL-VERSION
                 user
                 database
                 nil)

            result
            (result/make-result this)

            _
            (connection/send-message this message)

            messages
            (connection/read-messages-until this #{AuthenticationOk ErrorResponse})]

        (prot.result/handle result messages)))

    (initiate [this]

      (let [messages
            (connection/read-messages-until this #{ReadyForQuery})

            result
            (result/make-result this)]

        (prot.result/handle result messages)))

    (query [this sql opt]

      (let [message
            (new Query sql)

            messages
            (connection/read-messages-until this #{ReadyForQuery})

            result
            (result/make-result this opt)]

        (connection/send-message this message)
        (prot.result/handle result messages)))

    (terminate [this]

      (let [message
            (new Terminate)]

        (connection/send-message this message)
        (.close -ch)

        this))

    Closeable

    (close [this]
      (connection/terminate this)))


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
