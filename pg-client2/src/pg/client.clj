(ns pg.client
  (:import
   java.io.OutputStream
   java.util.UUID
   java.util.Map
   clojure.lang.Keyword
   com.github.igrishaev.Connection
   com.github.igrishaev.PreparedStatement
   com.github.igrishaev.Config$Builder
   com.github.igrishaev.enums.TXStatus
   com.github.igrishaev.enums.TxLevel))


(defn ->config ^Config$Builder [params]

  (let [{:keys [user
                database
                host
                port
                password
                protocol-version
                pg-params
                binary-encode?
                binary-decode?
                in-stream-buf-size
                out-stream-buf-size
                use-ssl?]}
        params]

    (cond-> (new Config$Builder user database)

      password
      (.password password)

      host
      (.host host)

      port
      (.port port)

      protocol-version
      (.protocolVersion protocol-version)

      pg-params
      (.pgParams pg-params)

      (some? binary-encode?)
      (.binaryEncode binary-encode?)

      (some? binary-decode?)
      (.binaryDecode binary-decode?)

      in-stream-buf-size
      (.inStreamBufSize in-stream-buf-size)

      out-stream-buf-size
      (.outStreamBufSize out-stream-buf-size)

      (some? use-ssl?)
      (.useSSL use-ssl?)

      :finally
      (.build))))


(defn connect ^Connection [config]
  (new Connection (->config config)))


(defn status ^Keyword [^Connection conn]
  (case (.getTxStatus conn)
    TXStatus/IDLE :I
    TXStatus/TRANSACTION :T
    TXStatus/ERROR :E
    nil nil))


(defn idle? ^Boolean [^Connection conn]
  (.isIdle conn))


(defn transaction? ^Boolean [^Connection conn]
  (.isTransaction conn))


(defn tx-error? ^Boolean [^Connection conn]
  (.isTxError conn))


(defn get-parameter
  ^String [^Connection conn ^String param]
  (.getParam conn param))


(defn get-parameters
  ^Map [^Connection conn]
  (.getParams conn))


(defn id ^UUID [^Connection conn]
  (.getId conn))


(defn pid ^Integer [^Connection conn]
  (.getPid conn))


(defn created-at
  ^Long [^Connection conn]
  (.getCreatedAt conn))


(defn close-statement
  [^Connection conn ^PreparedStatement stmt]
  (.closeStatement conn stmt))


(defn close-statement-by-name
  [^Connection conn ^String stmt-name]
  (.closeStatement conn stmt-name))


(defn close [^Connection conn]
  (.close conn))


(defmacro with-connection
  "
  Execute a block of code binding a connection
  to the `bind` symbol. Close the connection afterwards.
  "
  [[bind config] & body]
  `(let [~bind (connect ~config)]
     (try
       ~@body
       (finally
         (close ~bind)))))


(defn closed? [^Connection conn]
  (.isClosed conn))


(defn query [^Connection conn ^String sql]
  (.query conn sql))


(defn begin [^Connection conn]
  (.begin conn))


(defn commit [^Connection conn]
  (.commit conn))


(defn rollback [^Connection conn]
  (.rollback conn))


(defn clone ^Connection [^Connection conn]
  (Connection/clone conn))


(defn cancel-request [^Connection conn]
  (Connection/cancelRequest conn))


(defn copy-out
  [^Connection conn ^String sql ^OutputStream out]
  (.copyOut conn sql out))



(defmacro with-safe [& body]
  (try
    [(do ~@body) nil]
    (catch Throwable e#
      [nil e#])))


(defn ->tx-level ^TxLevel [^Keyword level]
  (case level

    :serializable
    TxLevel/SERIALIZABLE

    :repeatable-read
    TxLevel/REPEATABLE_READ

    :read-committed
    TxLevel/READ_COMMITTED

    :read-uncommitted
    TxLevel/READ_UNCOMMITTED))


(defmacro with-tx
  [[conn {:as opt :keys [isolation-level
                         read-only?
                         rollback?]}]
   & body]

  `(let [conn#
         ~conn

         level#
         (->tx-level ~isolation-level)]

     (.begin conn#)

     (when ~isolation-level
       (.setTxLevel conn# level#))

     (when ~read-only?
       (.setTxReadOnly conn#))

     (let [[result# e#]
           (with-safe ~@body)]

       (if e#
         (do
           (.rollback conn#)
           (throw e#))

         (do
           ~(if rollback?
              `(.rollback conn#)
              `(.commit conn#))
           result#)))))
