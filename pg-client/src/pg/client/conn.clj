(ns pg.client.conn
  (:import
   java.io.Closeable
   java.io.BufferedInputStream
   java.io.InputStream
   java.io.OutputStream
   java.io.Writer
   java.net.Socket
   java.util.HashMap
   java.util.Map)
  (:require
   [pg.bb :as bb]
   [pg.client.debug :as debug]
   [pg.client.msg :as msg]
   [pg.client.ssl :as ssl]
   [pg.coll :as coll]
   [pg.const :as const]))


(defn fn-notification-default [NotificationResponse]
  (println (format "PG notification: %s" NotificationResponse)))


(defn fn-notice-default [NoticeResponse]
  (println (format "PG notice: %s" NoticeResponse)))


(def config-defaults
  {:host "127.0.0.1"
   :port 5432
   :fn-notice fn-notice-default
   :fn-notification fn-notification-default
   :pg-params {}
   :protocol-version const/PROTOCOL_VERSION
   :binary-encode? false
   :binary-decode? false
   :ssl? false
   :ssl-context nil
   :socket {:tcp-no-delay? true
            :keep-alive? true
            :reuse-addr? true
            :reuse-port? true
            :rcv-buf nil
            :snd-buf nil}})


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


(defn get-ssl? [{:as conn :keys [^Map state]}]
  (.get state "ssl"))


(defn get-pid
  [{:keys [^Map state]}]
  (.get state "pid"))


(defn closed?
  ^Boolean [{:as conn :keys [^Socket socket]}]
  (.isClosed socket))


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


(defn send-message
  [{:as conn :keys [^OutputStream out-stream
                    opt]}
   message]
  (debug/debug-message message " ->")
  (let [bb (msg/encode-message message opt)]
    (.write out-stream (bb/array bb)))
  conn)


(defn send-password [conn ^String password]
  (let [msg (msg/make-PasswordMessage password)]
    (send-message conn msg))
  conn)


(defn terminate
  [conn]
  (send-message conn (msg/make-Terminate))
  (-> conn ^Socket (get :socket) .close)
  conn)


(defn send-sync [conn]
  (send-message conn (msg/make-Sync))
  conn)


(defn send-flush [conn]
  (send-message conn (msg/make-Flush))
  conn)


(defn read-message
  [{:keys [^InputStream in-stream
           opt]}]

  (let [buf-header
        (.readNBytes in-stream 5)

        bb-head
        (bb/wrap buf-header)

        tag
        (char (bb/read-byte bb-head))

        len
        (- (bb/read-int32 bb-head) 4)

        buf-body
        (.readNBytes in-stream len)

        bb-body
        (bb/wrap buf-body)

        message
        (msg/parse-message tag bb-body opt)]

    (debug/debug-message message "<- ")

    message))


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


(defn send-bind [conn statement params oids]

  (let [{:keys [config]}
        conn

        {:keys [binary-encode?
                binary-decode?]}
        config

        portal
        (name (gensym "portal_"))

        msg
        (msg/make-Bind portal
                       statement
                       params
                       oids
                       binary-encode?
                       binary-decode?)]

    (send-message conn msg)

    portal))


(defn send-execute [conn portal row-count]
  (let [msg (msg/make-Execute portal row-count)]
    (send-message conn msg)))


(defn send-copy-data [conn buffer]
  (let [msg (msg/make-CopyData buffer)]
    (send-message conn msg)))


(defn send-copy-done [conn]
  (let [msg (msg/make-CopyDone)]
    (send-message conn msg)))


(defn send-copy-fail
  ([conn]
   (let [msg (msg/make-CopyFail)]
     (send-message conn msg)))

  ([conn message]
   (let [msg (msg/make-CopyFail message)]
    (send-message conn msg))))


(defn send-ssl-request [conn]
  (let [msg (msg/make-SSLRequest const/SSL_CODE)]
    (send-message conn msg)))


(defn read-ssl-response ^Character [conn]
  (let [{:keys [^InputStream in-stream]}
        conn]
    (-> in-stream .read char)))


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
     ^Socket socket
     ^InputStream in-stream
     ^OutputStream out-stream
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

        (format "<PG connection %s@%s:%s/%s>"
                user host port database))))


(defmethod print-method Connection
  [conn ^Writer w]
  (.write w (str conn)))


(defn set-socket-opts
  [^Socket socket {:keys [tcp-no-delay?
                          timeout
                          keep-alive?
                          oob-inline?
                          reuse-addr?
                          rcv-buf
                          snd-buf]}]

  (when (some? tcp-no-delay?)
    (.setTcpNoDelay socket tcp-no-delay?))

  (when (some? oob-inline?)
    (.setOOBInline socket oob-inline?))

  (when (some? keep-alive?)
    (.setKeepAlive socket keep-alive?))

  (when (some? reuse-addr?)
    (.setReuseAddress socket reuse-addr?))

  (when (some? rcv-buf)
    (.setReceiveBufferSize socket rcv-buf))

  (when (some? snd-buf)
    (.setSendBufferSize socket snd-buf))

  (when (some? timeout)
    (.setSoTimeout socket timeout)))


(defn ssl-requested? ^Boolean [^Connection conn]
  (-> conn :config :ssl?))


(defn pre-ssl-stage ^Connection [^Connection conn]
  (if (ssl-requested? conn)
    (do
      (send-ssl-request conn)
      (case (read-ssl-response conn)
        \N
        (do
          (terminate conn)
          (throw (new Exception "SSL connection is not supported by the server")))
        \S
        (ssl/wrap-ssl conn)))
    conn))


(defn connect [config]

  (let [config-full
        (coll/deep-merge config-defaults config)

        {:keys [^String host
                ^Integer port]
         socket-opt :socket}
        config-full

        socket
        (new Socket host port true)

        in-stream
        (-> socket
            (.getInputStream)
            (BufferedInputStream. const/IN_STREAM_BUF_SIZE))

        out-stream
        (.getOutputStream socket)

        id
        (gensym "pg")

        created-at
        (System/currentTimeMillis)

        _
        (set-socket-opts socket socket-opt)

        conn
        (new Connection
             id
             created-at
             config-full
             socket
             in-stream
             out-stream
             (new HashMap)
             (new HashMap)
             (new HashMap))]

    (pre-ssl-stage conn)))
