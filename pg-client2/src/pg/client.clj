(ns pg.client
  (:require
   [clojure.string :as str])
  (:import
   clojure.lang.Keyword
   com.github.igrishaev.ConnConfig$Builder
   com.github.igrishaev.Connection
   com.github.igrishaev.ExecuteParams
   com.github.igrishaev.ExecuteParams$Builder
   com.github.igrishaev.PreparedStatement
   com.github.igrishaev.codec.DecoderBin
   com.github.igrishaev.codec.DecoderTxt
   com.github.igrishaev.codec.EncoderBin
   com.github.igrishaev.codec.EncoderTxt
   com.github.igrishaev.enums.CopyFormat
   com.github.igrishaev.enums.OID
   com.github.igrishaev.enums.TXStatus
   com.github.igrishaev.enums.TxLevel
   com.github.igrishaev.reducer.IReducer
   com.github.igrishaev.util.JSON
   com.github.igrishaev.util.JSON$Wrapper
   java.io.InputStream
   java.io.OutputStream
   java.io.Writer
   java.nio.ByteBuffer
   java.util.List
   java.util.Map
   java.util.UUID))


(def ^CopyFormat COPY_FORMAT_BIN CopyFormat/BIN)
(def ^CopyFormat COPY_FORMAT_CSV CopyFormat/CSV)
(def ^CopyFormat COPY_FORMAT_TAB CopyFormat/TAB)


(defn ->kebab ^Keyword [^String column]
  (-> column (str/replace #"_" "-") keyword))


(defn ->execute-params ^ExecuteParams [^Map opt]

  (let [{:keys [^List params
                oids
                row-count
                reducer
                fn-key
                output-stream
                input-stream
                group-by
                index-by
                matrix?
                kebab?
                fold
                run ;; TODO: each?
                init
                kv
                first?
                binary-encode?
                binary-decode?
                csv-null
                csv-sep
                csv-end
                copy-buf-size
                ^CopyFormat copy-format
                copy-csv?
                copy-bin?
                copy-tab?
                copy-in-rows
                copy-in-maps
                copy-in-keys]}
        opt]

    (cond-> (ExecuteParams/builder)

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

      input-stream
      (.inputStream input-stream)

      group-by
      (.groupBy group-by)

      index-by
      (.indexBy index-by)

      run
      (.run run)

      matrix?
      (.asMatrix)

      kebab?
      (.fnKeyTransform ->kebab)

      kv
      (.KV (first kv) (second kv))

      first?
      (.first)

      (and fold init)
      (.fold fold init)

      (some? binary-encode?)
      (.binaryEncode binary-encode?)

      (some? binary-decode?)
      (.binaryDecode binary-decode?)

      csv-null
      (.CSVNull csv-null)

      csv-sep
      (.CSVCellSep csv-sep)

      csv-end
      (.CSVLineSep csv-end)

      oids
      (.OIDs oids)

      copy-csv?
      (.setCSV)

      copy-bin?
      (.setBin)

      copy-tab?
      (.setBin)

      copy-format
      (.copyFormat copy-format)

      copy-buf-size
      (.bufSize copy-buf-size)

      copy-in-rows
      (.copyInRows copy-in-rows)

      copy-in-maps
      (.copyInMaps copy-in-maps)

      copy-in-keys
      (.copyMapKeys copy-in-keys)

      :finally
      (.build))))



(defn ->conn-config ^ConnConfig$Builder [params]

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

    (cond-> (new ConnConfig$Builder user database)

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
   (new Connection (->conn-config config)))

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
   (.prepare conn sql ExecuteParams/INSTANCE))

  (^PreparedStatement
   [^Connection conn ^String sql ^List oids]
   (.prepare conn sql oids)))


(defn execute-statement

  ([^Connection conn ^PreparedStatement stmt]
   (.executeStatement conn stmt ExecuteParams/INSTANCE))

  ([^Connection conn ^PreparedStatement stmt ^Map opt]
   (.executeStatement conn stmt (->execute-params opt))))


(defn execute

  ([^Connection conn ^String sql]
   (.execute conn sql ExecuteParams/INSTANCE))

  ([^Connection conn ^String sql ^Map opt]
   (.execute conn sql (->execute-params opt))))


(defmacro with-statement
  [[bind conn sql oids] & body]

  (let [CONN (gensym "CONN")]

    `(let [~CONN ~conn

           ~bind
           ~(if oids
              `(prepare-statement ~CONN ~sql ~oids)
              `(prepare-statement ~CONN ~sql))]

       (try
         ~@body
         (finally
           (close-statement ~CONN ~bind))))))


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
   (.query conn sql ExecuteParams/INSTANCE))

  ([^Connection conn ^String sql ^Map opt]
   (.query conn sql (->execute-params opt))))


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

  ([^Connection conn ^String sql ^OutputStream out]
   (copy-out conn sql out nil))

  ([^Connection conn ^String sql ^OutputStream out ^Map opt]
   (.copy conn
          sql
          (-> opt
              (assoc :output-stream out)
              (->execute-params)))))


(defn copy-in

  ([^Connection conn ^String sql ^InputStream in]
   (copy-in conn sql in nil))

  ([^Connection conn ^String sql ^InputStream in ^Map opt]
   (.copy conn
          sql
          (-> opt
              (assoc :input-stream in)
              (->execute-params)))))


(defn copy-in-rows

  ([^Connection conn ^String sql ^List rows]
   (copy-in-rows conn sql rows nil))

  ([^Connection conn ^String sql ^List rows ^Map opt]
   (.copy conn
          sql
          (-> opt
              (assoc :copy-in-rows rows)
              (->execute-params)))))


(defn copy-in-maps

  ([^Connection conn ^String sql ^List maps ^List keys]
   (copy-in-maps conn sql maps keys nil))

  ([^Connection conn ^String sql ^List maps ^List keys ^Map opt]
   (.copy conn
          sql
          (-> opt
              (assoc :copy-in-maps maps
                     :copy-in-keys keys)
              (->execute-params)))))


(defmacro with-safe [& body]
  `(try
     [(do ~@body) nil]
     (catch Throwable e#
       [nil e#])))


(defn ->tx-level ^TxLevel [level]
  (case level

    (:serializable "SERIALIZABLE")
    TxLevel/SERIALIZABLE

    (:repeatable-read "REPEATABLE READ")
    TxLevel/REPEATABLE_READ

    (:read-committed "READ COMMITTED")
    TxLevel/READ_COMMITTED

    (:read-uncommitted "READ UNCOMMITTED")
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


;;
;; JSON
;;

(defn json-wrap [x]
  (new JSON$Wrapper x))


(defn json-read-string [^String input]
  (JSON/readValue input))


(defn json-write-writer [value ^Writer writer]
  (JSON/writeValue writer value))


(defn json-write-stream [value ^OutputStream out ]
  (JSON/writeValue out value))


;;
;; Encode/decode
;;

(defn decode-bin [^ByteBuffer buf ^OID oid]
  (.rewind buf)
  (DecoderBin/decode buf oid))


(defn decode-txt [^String obj ^OID oid]
  (DecoderTxt/decode obj oid))


(defn encode-bin
  (^ByteBuffer [obj]
   (EncoderBin/encode obj))

  (^ByteBuffer [obj ^OID oid]
   (EncoderBin/encode obj oid)))


(defn encode-txt
  (^String [obj]
   (EncoderTxt/encode obj))

  (^String [obj ^OID oid]
   (EncoderTxt/encode obj oid)))
