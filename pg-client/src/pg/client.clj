(ns pg.client
  (:refer-clojure :exclude [group-by first])
  (:require
   [clojure.string :as str]
   [pg.client.conn :as conn]
   [pg.client.copy :as copy]
   [pg.client.quote :as quote]
   [pg.client.flow :as flow]
   [pg.client.sql :as sql]
   [pg.const :as const]
   [pg.oid :as oid]
   [pg.types.hint :as hint])
  (:import
   clojure.lang.Keyword
   java.util.HashMap
   java.util.List
   java.util.Map
   pg.client.conn.Connection))


(defn status
  "
  Get the current status of the connection as a keyword.
  "
  ^Keyword [conn]
  (conn/get-tx-status conn))


(defn idle?
  "
  True if the connection is idle at the moment.
  "
  ^Boolean [conn]
  (= (status conn) const/TX_IDLE))


(defn in-transaction?
  "
  True if the connection is in transaction at the moment.
  "
  ^Boolean [conn]
  (= (status conn) const/TX_TRANSACTION))


(defn tx-error?
  "
  True if an error had occurred in a transaction.
  "
  ^Boolean [conn]
  (= (status conn) const/TX_ERROR))


(defn get-parameter
  "
  Return a value of a connection parameter by its name
  (.e.g 'integer_datetimes', 'application_name', etc).
  "
  ^String [{:keys [^Map params]} ^String param]
  (.get params param))


(defn pid
  "
  Get the connection PID as an Integer.
  "
  ^Integer [conn]
  (conn/get-pid conn))


(defn prepare-statement
  "
  Prepare a statement from a SQL string. The query cannot have
  more that one expression (e.g. 'select this; select that'
  is not allowed).

  Args:
  - conn: the connection map;
  - sql:  a string SQL expression;
  - oids: (optional) a vector of OIDs to specify the types
          of the parameters; when not set, the OIDs a determined
          by Postgres.

  Return:
  - general information about the statement as a map
    (name, columns, params, etc).
  "

  (^Map [conn sql]
   (prepare-statement conn sql []))

  (^Map [conn sql oids]
   (let [statement
         (conn/send-parse conn sql oids)

         init
         {:statement statement}]

     (conn/describe-statement conn statement)
     (conn/send-sync conn)
     (flow/interact conn :prepare init))))


(defn close-statement
  "
  Close a previously prepared statement on the server side.
  "
  [conn stmt]
  (let [{:keys [statement]}
        stmt]
    (conn/close-statement conn statement))
  (conn/send-sync conn)
  (flow/interact conn :close-statement))


(defmacro with-statement
  "
  Execute a body in a prepare-/close-statement block.
  Bind a prepared statement to the `bind` symbol.
  "
  [[bind conn sql oids] & body]
  `(let [conn# ~conn
         sql# ~sql
         ~bind (prepare-statement conn# sql# ~oids)]
     (try
       ~@body
       (finally
         (close-statement conn# ~bind)))))


(defn authenticate
  "
  Run the authentication pipeline for a given connection.
  Returns the connection.
  "
  [conn]
  (conn/authenticate conn)
  (flow/interact conn :auth)
  conn)


(defn closed?
  "
  True if a connection has been closed.
  "
  ^Boolean [conn]
  (conn/closed? conn))


(defn ssl?
  "
  True if the connection is encrypted with SSL.
  "
  ^Boolean [conn]
  (conn/get-ssl? conn))


(defn connect
  "
  Having a connection config, establish a connection
  and pass the authentication pipeline.
  Returns a Connection object.
  "
  ^Connection [^Map config]
  (-> config
      (conn/connect)
      (authenticate)))


(defn terminate
  "
  Terminate a connection.
  "
  [conn]
  (when-not (closed? conn)
    (conn/terminate conn)))


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
         (terminate ~bind)))))


(defn clone
  "
  Create a new connection based on the config
  of the passed one.
  "
  ^Connection [{:as conn :keys [config]}]
  (connect config))


(defn cancel
  "
  Cancels a hanging query using a dedicated connection.
  A cancelled query will end up with an error response.
  "
  [conn]

  (let [master-conn
        (-> conn
            (get :config)
            (conn/connect))

        pid
        (conn/get-pid conn)

        secret-key
        (conn/get-secret-key conn)]

    (conn/cancel-request master-conn pid secret-key)
    (terminate master-conn)
    nil))


(defn id
  "
  Get a unique symbol assigned to a connection.
  "
  [conn]
  (conn/get-id conn))


(defn created-at
  "
  Return the connection created time in milliseconds.
  "
  ^Long [conn]
  (conn/get-created-at conn))


(defn query
  "
  Pefrorm a Simple Query request (see the link below).
  Takes a SQL string that might carry several expressions
  separated by a semicolon. Should there are more than one
  expression, the result will be a vector of results.

  Arguments:
  - `conn`: a Connection object;
  - `sql`: a string with (multiple) SQL expression(s).
  - `opt`: additional options.

  https://postgrespro.com/docs/postgrespro/14/protocol-flow#id-1.10.5.7.4
  "

  ([conn sql]
   (query conn sql nil))

  ([conn sql opt]
   (conn/send-query conn sql)
   (flow/interact conn :query opt)))


(defn execute-statement
  "
  Execute a prepared statement.

  Takes a previously prepared statement and a vector of parameters.
  Binds the parameters the statements, obtains a portal and reads
  the data from the portal. Closes the portal afterwards.

  Args:
  - conn:   the Connection object;
  - stmt:   a map, the result of the `prepare-statement` function;
  - params: a vector (seq) of params;
  - opt:    additional options.

  Options:
  - `:rows`: now many rows to fetch from the portal. The default is 0
    which means all rows.
  "

  ([conn stmt]
   (execute-statement conn stmt nil nil))

  ([conn stmt params]
   (execute-statement conn stmt params nil))

  ([conn stmt params opt]

   (let [rows
         (get opt :rows 0)

         {:keys [statement
                 ParameterDescription]}
         stmt

         {:keys [param-oids]}
         ParameterDescription

         portal
         (conn/send-bind conn statement params param-oids)]

     (conn/describe-portal conn portal)
     (conn/send-execute conn portal rows)
     (conn/close-portal conn portal)
     (conn/send-sync conn)
     (flow/interact conn :execute opt))))


(defn execute
  "
  Perform an Extended Query request (see the link below).

  The result depends on the expression and additional options.

  Args:
  - conn:   the Connection object;
  - sql:    a string with a single SQL expression (; is not allowed)
  - params: a vector (seq) of params;
  - opt:    additional options.

  https://postgrespro.com/docs/postgrespro/14/protocol-flow#PROTOCOL-FLOW-EXT-QUERY
  "

  ([conn sql]
   (execute conn sql nil nil))

  ([conn sql params]
   (execute conn sql params nil))

  ([conn sql params opt]
   (let [oids (mapv hint/hint params)]
     (with-statement [stmt conn sql oids]
       (execute-statement conn stmt params opt)))))


(defn begin
  "
  Open a transaction.
  "
  [conn]
  (query conn "BEGIN" nil))


(defn commit
  "
  Commit the current transaction.
  "
  [conn]
  (query conn "COMMIT" nil))


(defn rollback
  "
  Roll back the current transaction.
  "
  [conn]
  (query conn "ROLLBACK" nil))


(defmacro with-tx
  "
  Execute a body in a transaction block.

  Opens a transaction optionally setting its parameters.
  Executes the body block. Should an exception was caught,
  roll back the transaction and re-throw the exception.
  Otherwise, commit the transaction.

  Arguments:
  - conn: the Connection object;

  Options:
  - `read-only?`: pass true to open the transaction in read-only mode
    (no update/detele/etc expressions are allowed);

  - `isolation-level`: an isolation level of the transaction.
    Keywords, symbols and strings are allowed, e.g:
    `:repeatable-read`, `'REPEATABLE-READ'` (see `pg.client.sql`).

  - `rollback?`: true if the transaction must be rolled back even
    when no exception did appear. Useful for tests.
  "

  [[conn {:as opt :keys [read-only?
                         isolation-level
                         rollback?]}]
   & body]

  (let [bind (gensym "conn")]

    `(let [~bind ~conn]

       (begin ~bind)

       (let [pair#
             (try
               [nil (do
                      ~(when (or isolation-level read-only?)
                         `(when-let [sql# (sql/set-tx ~opt)]
                            (query ~bind sql# nil)))
                      ~@body)]
               (catch Throwable e#
                 [e# nil]))

             e#
             (get pair# 0)

             result#
             (get pair# 1)]

         (if e#
           (do
             (rollback ~bind)
             (throw e#))

           (do
             ~(if rollback?
                `(rollback ~bind)
                `(commit ~bind))
             result#))))))


(defn listen
  "
  Subscribe the connection to a given channel.
  "
  [conn ^String channel]
  (execute conn
           (format "LISTEN %s" (quote/quote'' channel))
           nil
           nil))


(defn unlisten
  "
  Unsbuscribe the connection from a given channel.
  "
  [conn ^String channel]
  (execute conn
           (format "UNLISTEN %s" (quote/quote'' channel))
           nil
           nil))


(defn notify
  "
  Send a text message to the given channel.
  "
  [conn ^String channel ^String message]
  (execute conn
           "select pg_notify($1, $2)"
           [(str/lower-case channel) message])
  nil)


;; ------------------

(defn copy-out
  "
  Copy a table or a result of a query into an output stream.
  The content might be either CSV or a binary dump depending on
  the SQL expression. Return a number of rows processed.
  "
  [conn sql output-stream]
  (query conn sql {:output-stream output-stream}))


(defn copy-in
  "
  Copy the data from an `InputStream` into the database.
  A `sql` string argument specifies all the details: a table,
  columns, format, header, etc. The payload might be either
  CSV or binary-encoded data (see Postgres binary encoding).
  Returns a number of rows processed.
  "

  ([conn sql input-stream]
   (copy-in conn sql input-stream nil))

  ([conn sql input-stream
    {:keys [buffer-size]
     :or {buffer-size const/COPY_BUFFER_SIZE}}]

   (copy/copy-in-stream conn sql input-stream buffer-size)))


(defn copy-in-rows
  "
  Copy a seq of rows into the database. Send each row directly to the
  database without producing an intermediate InputStream. Supports CSV
  and binary format types.

  Required params:
  - conn: a Connection object;
  - sql: a SQL expression that describes a table, columns, etc;
  - rows: a seq of rows; each row is a seq of values;

  Optional params:
  - sep: a charater to separate CSV columns, default is ,
  - end: a line-ending sequence of characters, default is \r\n
  - null: a string to represent NULL in CSV, default is an empty string;
  - oids: type hints for proper values encoding. Either a vector or OIDs
      or a map of {index => OID}. Examples: a vector [oid/int2 nil oid/date],
      a map {0 oid/int2, 2 oid/date}.
  - format: a keyword to to specify a format of a payload. Default is :csv;
      another supported format is :bin. This parameter affects the entire
      payload sent to the server.

  Returns a number of rows processed by the server.
  "
  ([conn sql rows]
   (copy-in-rows conn sql rows nil))

  ([conn sql rows {:keys [sep end oids format null]
                   :or {sep const/COPY_CSV_CELL_SEP
                        end const/COPY_CSV_LINE_SEP
                        null const/COPY_CSV_NULL
                        format const/COPY_FORMAT_CSV}}]
   (copy/copy-in-rows conn sql rows format oids sep end null)))


(defn copy-in-maps
  "
  Copy a seq of maps into the database. Acts like copy-in-rows but
  has turns maps into vectors first. The keys must be specified
  explicitly

  Required params: same as copy-in-rows plus:
    - keys: a vector of keys applied to each map via juxt;

  Optional params: same as copy-in-rows plus:
    - oids: a map of {key => OID} for proper encoding.

  Returns a number of rows processed by the server.
  "
  ([conn sql maps keys]
   (copy-in-maps conn sql maps keys nil))

  ([conn sql maps keys
    {:keys [sep end oids format null]
     :or {sep const/COPY_CSV_CELL_SEP
          end const/COPY_CSV_LINE_SEP
          null const/COPY_CSV_NULL
          format const/COPY_FORMAT_CSV}}]
   (copy/copy-in-maps conn sql maps keys format oids sep end null)))
