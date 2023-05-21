(ns pg.client.impl.connection
  (:require
   [pg.client.debug :as debug]
   [pg.client.auth.clear]
   [pg.client.auth.md5]
   [pg.client.bb :as bb]
   [pg.client.coll :as coll]
   [pg.client.impl.message]
   [pg.client.impl.result :as result]
   [pg.client.prot.connection :as connection]
   [pg.client.prot.message :as message]
   [pg.client.prot.result :as prot.result]
   [pg.error :as e])
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

    ;; TODO: accept a message!
    (send-message [this bb]
      (bb/write-to -ch bb))

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

          (debug/debug-message message ">>>")

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
            (new StartupMessage 196608 user database nil)

            bb
            (message/to-bb message this)

            result
            (result/result this)

            _
            (connection/send-message this bb)

            messages
            (connection/read-messages-until this #{AuthenticationOk ErrorResponse})]

        (prot.result/handle result messages)))

    (initiate [this]

      (let [messages
            (connection/read-messages-until this #{ReadyForQuery})

            result
            (result/result this)]

        (prot.result/handle result messages)))

    (query [this sql]

      (let [bb
            (-> (new Query sql)
                (message/to-bb this))

            messages
            (connection/read-messages-until this #{ReadyForQuery})

            result
            (result/result this)]

        (connection/send-message this bb)
        (prot.result/handle result messages)))

    (terminate [this]

      (let [bb
            (-> (new Terminate)
                (message/to-bb this))]

        (connection/send-message this bb)
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


(defmacro with-connection [[bind config] & body]
  `(let [~bind (connect ~config)]
     (try
       (connection/authenticate ~bind)
       (connection/initiate ~bind)
       ~@body
       (finally
         (connection/terminate ~bind)))))


#_
(comment

  (def -config {:host "127.0.0.1"
                :port 15432
                :user "foo"
                :password "foo"
                :database "ivan"})

  (with-connection [db -config]
    (connection/query db "select 1 as foo"))

  (with-connection [db -config]
    (connection/query db "create table aaa (id serial, title text)"))

  (with-connection [db -config]
    (connection/query db "select * from aaa"))

  (with-connection [db -config]
    (connection/query db "insert into aaa (title) values ('123')"))



)
