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
   pg.client.impl.message.Flush
   pg.client.impl.message.Parse
   pg.client.impl.message.ParseComplete
   pg.client.impl.message.Describe
   pg.client.impl.message.Bind
   pg.client.impl.message.BindComplete
   pg.client.impl.message.Execute
   pg.client.impl.message.Close
   pg.client.impl.message.CloseComplete
   pg.client.impl.message.Query
   pg.client.impl.message.ReadyForQuery
   pg.client.impl.message.StartupMessage
   pg.client.impl.message.Sync
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
      (debug/debug-message message "<- ")
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

          (debug/debug-message message " ->")

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

      (let [{:keys [database
                    user
                    pg-params
                    protocol-version]}
            -config

            message
            (new StartupMessage
                 protocol-version
                 user
                 database
                 pg-params)

            result
            (result/make-result this nil {:phase :authenticate})

            _
            (connection/send-message this message)

            messages
            (connection/read-messages-until this #{AuthenticationOk ErrorResponse})]

        (prot.result/handle result messages)))

    (initiate [this]

      (let [messages
            (connection/read-messages-until this #{ReadyForQuery ErrorResponse})

            result
            (result/make-result this nil {:phase :initiate})]

        (prot.result/handle result messages)))

    (send-sync [this]
      (connection/send-message this (new Sync)))

    (send-flush [this]
      (connection/send-message this (new Flush)))

    (handle-notice [this fields]
      (let [{:keys [fn-notice]}
            -config]
        (fn-notice fields)))

    (handle-notification [this NotificationResponse]
      (let [{:keys [fn-notification]}
            -config]
        (fn-notification NotificationResponse)))

    (query [this sql opt]

      (let [message
            (new Query sql)

            messages
            (connection/read-messages-until this #{ReadyForQuery})

            result
            (result/make-result this opt {:query sql})]

        (connection/send-message this message)
        (prot.result/handle result messages)))

    (terminate [this]

      (let [message
            (new Terminate)]

        (connection/send-message this message)
        (.close -ch)

        this))

    (parse [this query]

      (let [statement
            (name (gensym "statement_"))

            message
            (new Parse statement query [])]

        (connection/send-message this message)

        statement))

    (bind [this statement params]

      (let [portal
            (name (gensym "portal_"))

            message
            (new Bind portal statement [] params [])]

        (connection/send-message this message)

        portal))

    (execute [this portal]

      (let [message
            (new Execute portal 0)

            messages
            (connection/read-messages-until this #{ReadyForQuery ErrorResponse})

            result
            (result/make-result this nil nil)]

        (connection/send-message this message)
        (prot.result/handle result messages)))

    (close-statement [this statement]

      (let [message
            (new Close \S statement)]
        (connection/send-message this message))

      nil)

    (close-portal [this portal]

      (let [message
            (new Close \P portal)]
        (connection/send-message this message))

      nil)

    (describe-statement [this statement-name]

      (let [message-describe
            (new Describe \S statement-name)

            message-flush
            (new Flush)

            messages
            (connection/read-messages-until this #{ReadyForQuery ErrorResponse})

            ex-data
            {:op :Describe
             :statement statement-name}

            result
            (result/make-result this nil ex-data)]

        (connection/send-message this message-describe)
        (connection/send-message this message-flush)
        (prot.result/handle result messages)))

    (describe-portal [this portal-name])

    Closeable

    (close [this]
      (connection/terminate this)))


(defn fn-notice-default [fields]
  (println "PG NOTICE:" fields))


(defn fn-notification-default [NotificationResponse]
  (println "PG NOTIFICATION:" NotificationResponse))


(def config-defaults
  {:host "127.0.0.1"
   :port 5432
   :fn-notice fn-notice-default
   :fn-notification fn-notification-default
   :protocol-version const/PROTOCOL-VERSION})


(defn connect [config]

  (let [config+
        (merge config-defaults config)

        {:keys [^String host
                ^Integer port]}
        config+

        addr
        (new java.net.InetSocketAddress host port)

        ch
        (SocketChannel/open addr)]

    (new Connection
         config+
         addr
         ch
         (new HashMap)
         (new HashMap))))
