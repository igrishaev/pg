(ns pg.client
  (:require
   [pg.client.conn :as conn]
   [pg.client.func :as func]
   [pg.client.result :as res]
   [pg.client.sql :as sql]
   [pg.const :as const]
   [pg.hint :as hint]
   [pg.oid :as oid])
  (:import
   clojure.lang.Keyword
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
  Prepare a statement from an SQL expression.
  Args:
  - conn: the connection map;
  - sql: a string SQL expression;
  - oids (optional): a vector of OIDs to specify the types
    of the parameters; when not set, the OIDs a determined
    by Postgres.
  Return:
  - a map with brief information about the statement
    (name, columns, params).
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
     (res/interact conn :prepare init))))


(defn close-statement
  "
  Close a previously prepared statement on the server side.
  "
  [conn ^Map stmt]
  (let [{:keys [statement]}
        stmt]
    (conn/close-statement conn statement))
  (conn/send-sync conn)
  (res/interact conn :close-statement))


(defmacro with-statement
  [[bind conn sql oids] & body]
  `(let [conn# ~conn
         sql# ~sql
         ~bind (prepare-statement conn# sql# ~oids)]
     (try
       ~@body
       (finally
         (close-statement conn# ~bind)))))


(defn authenticate [conn]
  (conn/authenticate conn)
  (res/interact conn :auth)
  conn)


(defn closed? [conn]
  (conn/get-closed conn))


(defn connect ^Connection [^Map config]
  (-> config
      (conn/connect)
      (authenticate)))


(defn terminate [conn]
  (when-not (closed? conn)
    (conn/terminate conn)))


(defmacro with-connection
  [[bind config] & body]
  `(let [~bind (connect ~config)]
     (try
       ~@body
       (finally
         (terminate ~bind)))))


(defn clone [{:as conn :keys [config]}]
  (connect config))


(defn cancel-request

  ([{:as conn :keys [config]}]

   (with-connection [new-conn config]
     (cancel-request new-conn conn)))

  ([conn conn-to-cancel]

   (let [pid
         (conn/get-pid conn-to-cancel)

         secret-key
         (conn/get-secret-key conn-to-cancel)]

     (conn/cancel-request conn pid secret-key))))


(defn id [conn]
  (conn/get-id conn))


(defn created-at [conn]
  (conn/get-created-at conn))


(defn query

  ([conn sql]
   (query conn sql nil))

  ([conn sql opt]
   (conn/send-query conn sql)
   (res/interact conn :query opt)))


(defn execute-statement

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
     (res/interact conn :execute opt))))


(defn execute

  ([conn sql]
   (execute conn sql nil nil))

  ([conn sql params]
   (execute conn sql params nil))

  ([conn sql params opt]
   (let [oids (mapv hint/hint params)]
     (with-statement [stmt conn sql oids]
       (execute-statement conn stmt params opt)))))


(defn begin [conn]
  (execute conn "BEGIN" nil nil))


(defn commit [conn]
  (execute conn "COMMIT" nil nil))


(defn rollback [conn]
  (execute conn "ROLLBACK" nil nil))


(defmacro with-tx

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
