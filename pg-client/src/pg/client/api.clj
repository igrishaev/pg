(ns pg.client.api
  (:require
   [pg.oid :as oid]
   [pg.client.sql :as sql]
   [pg.client.conn :as conn]
   [pg.client.result :as res])
  (:import
   java.util.Map
   java.util.List
   pg.client.conn.Connection))


(defn status [conn]
  (conn/get-tx-status conn))


(defn get-parameter
  [{:keys [^Map params]} ^String param]
  (.get params param))


(defn pid [conn]
  (conn/get-pid conn))


(defn prepare

  ([conn sql]
   (prepare conn sql []))

  ([conn sql oids]
   (let [statement
         (conn/send-parse conn sql oids)

         init
         {:statement statement}]

     (conn/describe-statement conn statement)
     (conn/send-sync conn)
     (res/interact conn :prepare init))))


(defn close-statement [conn ^Map stmt]
  (let [{:keys [statement]}
        stmt]
    (conn/close-statement conn statement))
  (conn/send-sync conn)
  (res/interact conn :close-statement))


(defmacro with-statement
  [[bind conn sql oids] & body]
  `(let [conn# ~conn
         sql# ~sql
         ~bind (prepare conn# sql# ~oids)]
     (try
       ~@body
       (finally
         (close-statement conn# ~bind)))))


(defn authenticate [conn]
  (conn/authenticate conn)
  (res/interact conn :auth)
  conn)


(defn connect ^Connection [^Map config]
  (-> config
      (conn/connect)
      (authenticate)))


(defn terminate [conn]
  (conn/terminate conn))


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

  ([conn query]
   (execute conn query nil))

  ([conn query opt]

   (cond

     (string? query)
     (do
       (conn/send-query conn query)
       (res/interact conn :query opt))

     (vector? query)
     (let [[sql & params]
           query

           oids
           (mapv oid/value->oid params)]

       (with-statement [stmt conn sql oids]
         (execute-statement conn stmt params opt)))

     :else
     (throw (ex-info "wrong query type"
                     {:query query :opt opt})))))


(defn begin [conn]
  (execute conn "BEGIN" nil))


(defn commit [conn]
  (execute conn "COMMIT" nil))


(defn rollback [conn]
  (execute conn "ROLLBACK" nil))


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
                         `(when-let [query# (sql/set-tx ~opt)]
                            (execute ~bind query# nil)))
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


#_
(comment

  (def -cfg {:host "localhost"
             :port 15432
             :user "ivan"
             :database "ivan"
             :password "ivan"})

  (with-connection [c -cfg]
    (query c "select 1"))

  (def -conn (connect -cfg))

  (with-statement [stmt -conn "select $1::integer as one"]
    stmt)

  (with-statement [stmt -conn "select 1 as one, 2 as two"]
    (execute -conn stmt [] 0))

  (def -r (query -conn "select 1 as foo; select 2 as bar"))

  (def -s (prepare -conn "select $1::integer as kek from generate_series(1, 3)"))

  (def -p (execute -conn -s [1] 1))

  )
