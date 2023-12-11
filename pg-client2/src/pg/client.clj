(ns pg.client
  (:import
   java.io.Writer
   java.io.OutputStream
   java.util.UUID
   java.util.Map
   java.util.List
   clojure.lang.Keyword
   com.github.igrishaev.reducer.IReducer
   com.github.igrishaev.Connection
   com.github.igrishaev.ExecuteParams
   com.github.igrishaev.ExecuteParams$Builder
   com.github.igrishaev.PreparedStatement
   com.github.igrishaev.Config$Builder
   com.github.igrishaev.Result
   com.github.igrishaev.enums.TXStatus
   com.github.igrishaev.enums.TxLevel))


(defn ->execute-params ^ExecuteParams [config]

  (let [{:keys [params
                oids
                row-count
                reducer
                fn-key
                output-stream
                group-by
                index-by
                matrix?
                fold
                kv
                binary-encode?
                binary-decode?]}
        config]

    (cond-> (new ExecuteParams$Builder)

      params
      (.params params)

      oids
      (.OIDs oids)

      reducer
      (.reducer reducer)

      row-count
      (.rowCount row-count)

      fn-key
      (.fnKeyTransform fn-key)

      output-stream
      (.outputStream output-stream)

      group-by
      (.groupBy group-by)

      index-by
      (.indexBy index-by)

      matrix?
      (.asMatrix)

      kv
      (.KV (first kv) (second kv))

      fold
      (.fold (first fold) (second fold))

      (some? binary-encode?)
      (.binaryEncode binary-encode?)

      (some? binary-decode?)
      (.binaryDecode binary-decode?)


      :finally
      (.build))))


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
                fn-notification
                fn-protocol-version
                fn-notice
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

      fn-notification
      (.fnNotification fn-notification)

      fn-protocol-version
      (.fnProtocolVersion fn-protocol-version)

      fn-notice
      (.fnNotice fn-notice)

      :finally
      (.build))))


(defn connect

  (^Connection [config]
   (new Connection (->config config)))

  (^Connection [^String host ^Integer port ^String user ^String password ^String database]
   (new Connection host port user password database)))


(let [-mapping
      {TXStatus/IDLE :I
       TXStatus/TRANSACTION :T
       TXStatus/ERROR :E}]

  (defn status ^Keyword [^Connection conn]
    (get -mapping (.getTxStatus conn))))


(defn idle? ^Boolean [^Connection conn]
  (.isIdle conn))


(defn in-transaction? ^Boolean [^Connection conn]
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


(defn close [^Connection conn]
  (.close conn))


(defn ssl?
  "
  True if the connection is encrypted with SSL.
  "
  ^Boolean [^Connection conn]
  (.isSSL conn))


(defn prepare-statement

  (^PreparedStatement
   [^Connection conn ^String sql]
   (.prepare conn sql))

  (^PreparedStatement
   [^Connection conn ^String sql ^List oids]
   (.prepare conn sql oids)))


(defn Results->clj [^List results]
  (cond

    (.isEmpty results)
    nil

    (= 1 (.size results))
    (-> results ^Result (.get 0) .result)

    :else
    (mapv (fn [^Result result]
            (.result result)) results)))


(defn execute-statement

  ([^Connection conn ^PreparedStatement stmt]
   (Results->clj
    (.executeStatement conn stmt)))

  ([^Connection conn ^PreparedStatement stmt ^Map params]
   (Results->clj
    (.executeStatement conn stmt (->execute-params params)))))


(defn execute

  ([^Connection conn ^String sql]
   (Results->clj
    (.execute conn sql)))

  ([^Connection conn ^String sql ^Map params]
   (Results->clj
    (.execute conn sql (->execute-params params)))))


(defmacro with-statement
  [[bind conn sql oids] & body]

  `(let [conn#
         ~conn

         sql#
         ~sql

         ~bind
         ~(if oids
            `(prepare-statement conn# sql# ~oids)
            `(prepare-statement conn# sql#))]

     (try
       ~@body
       (finally
         (close-statement conn# ~bind)))))


(defmacro with-connection
  [[bind config] & body]
  `(let [~bind (connect ~config)]
     (try
       ~@body
       (finally
         (close ~bind)))))


(defn closed? [^Connection conn]
  (.isClosed conn))


(defn query

  ([^Connection conn ^String sql]
   (Results->clj
    (.query conn sql)))

  ([^Connection conn ^String sql ^Map params]
   (Results->clj
    (.query conn sql (->execute-params params)))))


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
  `(try
     [(do ~@body) nil]
     (catch Throwable e#
       [nil e#])))


(defn ->tx-level ^TxLevel [^Keyword level]
  (case level

    (:serializable "serializable")
    TxLevel/SERIALIZABLE

    (:repeatable-read "repeatable-read")
    TxLevel/REPEATABLE_READ

    (:read-committed "read-committed")
    TxLevel/READ_COMMITTED

    (:read-uncommitted "read-uncommitted")
    TxLevel/READ_UNCOMMITTED))


(defmacro with-tx
  [[conn {:as opt :keys [isolation-level
                         read-only?
                         rollback?]}]
   & body]

  (let [bind (gensym "CONN")]

    `(let [~bind ~conn]

       (.begin ~bind)

       ~@(when isolation-level
           [`(.setTxLevel ~bind (->tx-level ~isolation-level))])

       ~@(when read-only?
           [`(.setTxReadOnly ~bind)])

       (let [[result# e#]
             (with-safe ~@body)]

         (if e#
           (do
             (.rollback ~bind)
             (throw e#))

           (do
             ~(if rollback?
                `(.rollback ~bind)
                `(.commit ~bind))
             result#))))))



(defn connection? [x]
  (instance? Connection x))


(defmethod print-method Connection
  [^Connection conn ^Writer writer]
  (.write writer (.toString conn)))


(defn listen
  "
  Subscribe the connection to a given channel.
  "
  [^Connection conn ^String channel]
  (.listen conn channel))


(defn unlisten
  "
  Unsbuscribe the connection from a given channel.
  "
  [^Connection conn ^String channel]
  (.unlisten conn channel))


(defn notify
  "
  Send a text message to the given channel.
  "
  [^Connection conn ^String channel ^String message]
  (.notify conn channel message))


(defn prepared-statement? [x]
  (instance? PreparedStatement x))
