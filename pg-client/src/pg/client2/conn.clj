(ns pg.client2.conn
  (:refer-clojure :exclude [flush sync])
  (:import
   java.io.Closeable
   java.nio.channels.SocketChannel
   java.net.InetSocketAddress
   java.nio.ByteBuffer
   java.util.HashMap
   java.util.Map)
  (:require
   [pg.client2.const :as const]
   [pg.client2.debug :as debug]
   [pg.client2.bb :as bb]
   [pg.client2.msg :as msg]
   [pg.client2.const :as const]))


(def config-defaults
  {:host "127.0.0.1"
   :port 5432
   ;; :fn-notice fn-notice-default
   ;; :fn-notification fn-notification-default
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


(defn set-tx-status [this tx-status])


(defn get-tx-status [this])


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


(defn get-server-encoding [this])


(defn get-client-encoding ^String [this])


(defn get-password [this])


(defn get-user [conn]
  (-> conn :config :user))


(defn get-database [conn]
  (-> conn :config :database))


(defn sync [this])


(defn flush [this])


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


(defn send-message [{:keys [ch]} message]
  (debug/debug-message message "<- ")
  ;; TODO: options
  (let [bb (msg/encode-message message {})]
    (bb/write-to ch bb)))


(defn authenticate [conn]

  (let [user
        (get-user conn)

        database
        (get-database conn)

        msg (msg/make-StartupMessage
             const/PROTOCOL_VERSION
             user
             database
             ;; TODO: options
             nil)]

    (send-message conn msg)))


(defn query [conn sql]
  (let [msg (msg/make-Query sql)]
    (send-message conn msg)))


(defn terminate [this])


(defn parse [this query])


(defn bind [this statement params])


(defn execute [this portal row-count])


(defn close-statement [this statement-name])


(defn close-portal [this portal-name])


(defn describe-statement [this statement-name])


(defn describe-portal [this portal-name])
