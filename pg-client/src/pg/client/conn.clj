(ns pg.client.conn
  (:refer-clojure :exclude [flush sync])
  (:import
   java.io.Closeable
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
  (println NotificationResponse))


(def config-defaults
  {:host "127.0.0.1"
   :port 5432
   ;; :fn-notice fn-notice-default
   :fn-notification fn-notification-default
   :protocol-version const/PROTOCOL_VERSION})


(defn connect [config]

  ;; TODO: conn params

  (let [config-full
        (merge config-defaults config)

        {:keys [^String host
                ^Integer port]}
        config-full

        addr
        (new java.net.InetSocketAddress host port)

        ch
        (SocketChannel/open addr)]

    {:config config-full
     :addr addr
     :ch ch
     :params (new HashMap)
     :state (new HashMap)}))


(defn handle-notification [conn NotificationResponse]
  (when-let [fn-notification
             (-> conn :config :fn-notification)]
    (fn-notification NotificationResponse)))


(defn set-pid
  [{:as conn :keys [^Map state]}
   ^Integer pid]
  (.put state "pid" pid)
  conn)


(defn get-pid
  [{:keys [^Map state]}]
  (.get state "pid"))


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


(defn send-message [{:as conn :keys [ch]} message]
  (debug/debug-message message "<- ")
  ;; TODO: options
  (let [bb (msg/encode-message message {})]
    (bb/write-to ch bb))
  conn)


(defn send-password [conn ^String password]
  (send-message (msg/make-PasswordMessage password))
  conn)


(defn terminate
  [{:as conn :keys [^SocketChannel ch]}]
  (send-message conn (msg/make-Terminate))
  (.close ch)
  conn)


(defn send-sync [conn]
  (send-message conn (msg/make-Sync))
  conn)


(defn send-flush [conn]
  (send-message conn (msg/make-Flush))
  conn)


(defn read-message
  [{:keys [ch]}]

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

          ;; TODO options
          message
          (msg/parse-message tag bb-body nil)]

      (debug/debug-message message " ->")

      message)))


(defn authenticate [conn]

  (let [user
        (get-user conn)

        database
        (get-database conn)

        params
        (get-pg-params conn)

        msg (msg/make-StartupMessage
             const/PROTOCOL_VERSION
             user
             database
             params)]

    (send-message conn msg)))


(defn send-query [conn sql]
  (let [msg (msg/make-Query sql)]
    (send-message conn msg)))


;; TODO: pass oids
(defn send-parse [conn query]

  (let [statement
        (name (gensym "statement_"))

        msg
        (msg/make-Parse statement query [])]

    (send-message conn msg)

    statement))


(defn send-bind [conn statement params param-oids]

  (let [portal
        (name (gensym "portal_"))

        msg
        ;; TODO better formats
        (msg/make-Bind portal statement [0] params param-oids [0])]

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
