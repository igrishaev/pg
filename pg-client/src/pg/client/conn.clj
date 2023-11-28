(ns pg.client.conn

  (:import
   com.github.igrishaev.Connection)

  (:import
   java.io.Closeable
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
   [pg.const :as const]
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


(defn handle-notification
  [^Connection conn NotificationResponse]
  (when-let [fn-notification
             (-> conn (.getConfig) :fn-notification)]
    (fn-notification NotificationResponse)))


(defn handle-notice [^Connection conn NoticeResponse]
  (when-let [fn-notice
             (-> conn (.getConfig) :fn-notice)]
    (fn-notice NoticeResponse)))


(defn get-opt [conn]
  {})


(defn get-server-encoding ^String [conn]
  "UTF-8"
  #_
  (get-in conn [:params "server_encoding"] "UTF-8"))


(defn get-client-encoding ^String [conn]
  "UTF-8"
  #_
  (get-in conn [:params "client_encoding"] "UTF-8"))


(defn get-pg-params [^Connection conn]
  (-> conn (.getConfig) :pg-params))


(defn get-protocol-version [^Connection conn]
  (-> conn (.getConfig) :protocol-version))


(defn send-message
  [^Connection conn message]
  (debug/debug-message message " ->")

  (let [out-stream
        (.getOutputStream conn)

        bb
        (msg/encode-message message {})]

    (.write out-stream (bb/array bb)))
  conn)


(defn read-message
  [^Connection conn]

  (let [in-stream
        (.getInputStream conn)

        ;; TODO:
        opt
        {}

        buf-header
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


(defn send-bind [^Connection conn statement params oids]

  (let [config
        (.getConfig conn)

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
      (.sendSSLRequest conn)
      (case (read-ssl-response conn)
        \N
        (do
          (.close conn)
          (throw (new Exception "SSL connection is not supported by the server")))
        \S
        (ssl/wrap-ssl conn)))
    conn))


(defn connect ^Connection [config]

  (let [config-full
        (coll/deep-merge config-defaults config)

        ;; {:keys [^String host
        ;;         ^Integer port]
        ;;  socket-opt :socket}
        ;; config-full

        ;; socket
        ;; (new Socket host port true)

        ;; in-stream
        ;; (.getInputStream socket)

        ;; out-stream
        ;; (.getOutputStream socket)

        ;; id
        ;; (gensym "pg")

        ;; created-at
        ;; (System/currentTimeMillis)

        ;; _
        ;; (set-socket-opts socket socket-opt)

        ;; conn
        ;; (new Connection
        ;;      id
        ;;      created-at
        ;;      config-full
        ;;      socket
        ;;      in-stream
        ;;      out-stream
        ;;      (new HashMap)
        ;;      (new HashMap)
        ;;      (new HashMap))

]

    (new Connection config-full)

#_
    (pre-ssl-stage conn)))


(defmethod print-method Connection
  [conn ^Writer w]
  (.write w (str conn)))
