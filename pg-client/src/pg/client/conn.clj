(ns pg.client.conn
  (:import
   java.io.Closeable
   java.io.Writer
   java.nio.channels.SocketChannel
   java.net.InetSocketAddress
   java.nio.ByteBuffer
   java.util.HashMap
   java.util.Map)
  (:require
   [pg.client.const :as const]
   [pg.client.debug :as debug]
   [pg.client.bb :as bb]
   [pg.client.msg :as msg]
   [pg.client.const :as const]))


(defn fn-notification-default [NotificationResponse]
  (println (format "PG notification: %s" NotificationResponse)))


(defn fn-notice-default [NoticeResponse]
  (println (format "PG notice: %s" NoticeResponse)))


(def config-defaults
  {:host "127.0.0.1"
   :port 5432
   :fn-notice fn-notice-default
   :fn-notification fn-notification-default
   :protocol-version const/PROTOCOL_VERSION
   :binary-encode? false
   :binary-decode? false})


(defn get-id [conn]
  (:id conn))


(defn get-created-at [conn]
  (:created-at conn))


(defn handle-notification [conn NotificationResponse]
  (when-let [fn-notification
             (-> conn :config :fn-notification)]
    (fn-notification NotificationResponse)))


(defn handle-notice [conn NoticeResponse]
  (when-let [fn-notice
             (-> conn :config :fn-notice)]
    (fn-notice NoticeResponse)))


(defn set-pid
  [{:as conn :keys [^Map state]}
   ^Integer pid]
  (.put state "pid" pid)
  conn)


(defn get-pid
  [{:keys [^Map state]}]
  (.get state "pid"))


(defn set-closed
  [{:as conn :keys [^Map state]}]
  (.put state "closed" true)
  conn)


(defn get-closed
  [{:as conn :keys [^Map state]}]
  (.get state "closed"))


(defn set-secret-key
  [{:as conn :keys [^Map state]}
   ^Integer secret-key]
  (.put state "secret-key" secret-key)
  conn)


(defn get-secret-key
  [{:keys [^Map state]}]
  (.get state "secret-key"))


(defn set-tx-status
  [{:as conn :keys [^Map state]} tx-status]
  (.put state "tx-status" tx-status)
  conn)


(defn get-tx-status
  [{:as conn :keys [^Map state]}]
  (.get state "tx-status"))


(defn set-parameter
  [{:as conn :keys [^Map params]}
   ^String param
   ^String value]
  (.put params param value)
  conn)


(defn get-parameter
  [{:keys [^Map params]}
   ^String param]
  (.get params param))


(defn get-server-encoding ^String [conn]
  (get-in conn [:params "server_encoding"] "UTF-8"))


(defn get-client-encoding ^String [conn]
  (get-in conn [:params "client_encoding"] "UTF-8"))


(defn get-password [conn]
  (-> conn :config :password))


(defn get-user [conn]
  (-> conn :config :user))


(defn get-database [conn]
  (-> conn :config :database))


(defn get-pg-params [conn]
  (-> conn :config :pg-params))


(defn get-protocol-version [conn]
  (-> conn :config :protocol-version))


(defn send-message [{:as conn :keys [opt ch]} message]
  (debug/debug-message message "<- ")
  (let [bb (msg/encode-message message opt)]
    (bb/write-to ch bb))
  conn)


(defn send-password [conn ^String password]
  (let [msg (msg/make-PasswordMessage password)]
    (send-message conn msg))
  conn)


(defn terminate
  [{:as conn :keys [^SocketChannel ch]}]
  (send-message conn (msg/make-Terminate))
  (.close ch)
  (set-closed conn)
  conn)


(defn send-sync [conn]
  (send-message conn (msg/make-Sync))
  conn)


(defn send-flush [conn]
  (send-message conn (msg/make-Flush))
  conn)


(defn read-message
  [{:keys [ch opt]}]

  (let [bb-head
        (bb/allocate 5)]

    (bb/read-from ch bb-head)

    (let [tag
          (char (bb/read-byte bb-head))

          len
          (- (bb/read-int32 bb-head) 4)

          bb-body
          (bb/allocate len)

          _
          (bb/read-from ch bb-body)

          message
          (msg/parse-message tag bb-body opt)]

      (debug/debug-message message " ->")

      message)))


(defn authenticate [conn]

  (let [user
        (get-user conn)

        database
        (get-database conn)

        params
        (get-pg-params conn)

        protocol-version
        (get-protocol-version conn)

        msg
        (msg/make-StartupMessage
         protocol-version
         user
         database
         params)]

    (send-message conn msg)))


(defn send-query [conn sql]
  (let [msg (msg/make-Query sql)]
    (send-message conn msg)))


(defn send-parse [conn query oids]

  (let [statement
        (name (gensym "statement_"))

        msg
        (msg/make-Parse statement query oids)]

    (send-message conn msg)

    statement))


(defn send-bind [conn statement params param-oids]

  (let [{:keys [config]}
        conn

        {:keys [binary-encode?
                binary-decode?]}
        config

        param-formats
        (if binary-encode? [1] [0])

        column-formats
        (if binary-decode? [1] [0])

        portal
        (name (gensym "portal_"))

        msg
        (msg/make-Bind portal
                       statement
                       param-formats
                       params
                       param-oids
                       column-formats)]

    (send-message conn msg)

    portal))


(defn send-execute [conn portal row-count]
  (let [msg
        (msg/make-Execute portal row-count)]
    (send-message conn msg)))


(defn close-statement
  [conn ^String statement]
  (send-message conn (msg/make-Close \S statement)))


(defn close-portal
  [conn ^String portal]
  (send-message conn (msg/make-Close \P portal)))


(defn cancel-request [conn pid secret-key]
  (let [msg (msg/make-CancelRequest const/CANCEL_CODE
                                    pid
                                    secret-key)]
    (send-message conn msg)
    conn))


(defn describe-statement [conn ^String statement]
  (let [msg (msg/make-Describe \S statement)]
    (send-message conn msg)))


(defn describe-portal [conn ^String portal]
  (let [msg (msg/make-Describe \P portal)]
    (send-message conn msg)))


(defn rebuild-opt [{:keys [^Map opt]} param value]
  (case param

    "server_encoding"
    (.put opt :server-encoding value)

    "client_encoding"
    (.put opt :client-encoding value)

    "DateStyle"
    (.put opt :date-style value)

    "TimeZone"
    (.put opt :time-zone value)

    nil))


(defn get-opt [conn]
  (:opt conn))


(defrecord Connection
    [^String id
     ^Long created-at
     ^Map config
     ^InetSocketAddress addr
     ^SocketChannel ch
     ^Map params
     ^Map state
     ^Map opt]

    Closeable

    (close [this]
      (terminate this))

    Object

    (toString [_]

      (let [{:keys [host
                    port
                    user
                    database]}
            config]

        (format "PG connection %s@%s:%s/%s"
                user host port database))))


(defmethod print-method Connection
  [conn ^Writer w]
  (.write w (str conn)))


(defn connect [config]

  (let [config-full
        (merge config-defaults config)

        {:keys [^String host
                ^Integer port]}
        config-full

        addr
        (new java.net.InetSocketAddress host port)

        ch
        (SocketChannel/open addr)

        id
        (gensym "pg")

        created-at
        (System/currentTimeMillis)]

    (new Connection
         id
         created-at
         config-full
         addr
         ch
         (new HashMap)
         (new HashMap)
         (new HashMap))))
