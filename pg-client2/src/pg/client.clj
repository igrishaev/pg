(ns pg.client
  (:import
   java.util.UUID
   java.lang.Keyword
   com.github.igrishaev.Connection
   com.github.igrishaev.PreparedStatement
   com.github.igrishaev.Config$Builder
   com.github.igrishaev.enums.TXStatus))


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


(defn idle? ^boolean [^Connection conn]
  (.isIdle conn))


(defn transaction? ^boolean [^Connection conn]
  (.isTransaction conn))


(defn tx-error? ^boolean [^Connection conn]
  (.isTxError conn))


(defn get-parameter
  ^String [^Connection conn ^String param]
  (.getParam conn param))


(defn get-parameters
  ^String [^Connection conn]
  (.getParams conn param))


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


(defn query [^Connection conn ^String sql]
  (.query conn sql))


(defn begin [^Connection conn]
  (.begin conn))


(defn commit [^Connection conn]
  (.commit conn))


(defn rollback [^Connection conn]
  (.rollback conn))
