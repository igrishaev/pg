(ns pg.client.api
  (:import
   java.util.Map
   java.util.List)
  (:require
   [pg.client.sql :as sql]
   [pg.client.conn :as conn]
   [pg.client.result :as res]))


(defn status [conn]
  (conn/get-tx-status conn))


(defn query
  ([conn sql]
   (query conn sql nil))

  ([conn sql opt]
   (conn/send-query conn sql)
   (res/interact conn :query opt)))


(defn get-parameter [{:keys [^Map params]} ^String param]
  (.get params param))


(defn begin [conn]
  (query conn "BEGIN"))


(defn commit [conn]
  (query conn "COMMIT"))


(defn rollback [conn]
  (query conn "ROLLBACK"))


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
                            (query ~bind query#)))
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


(defn pid [conn]
  (conn/get-pid conn))


(defn prepare [conn sql]

  (let [statement
        (conn/send-parse conn sql)

        init
        {:statement statement}]

    (conn/describe-statement conn statement)
    (conn/send-sync conn)
    (res/interact conn :prepare init)))


(defn execute [conn
               ^Map Statement
               ^List params
               ^Integer row-count]

  (let [{:keys [statement
                ParameterDescription]}
        Statement

        {:keys [param-oids]}
        ParameterDescription

        portal
        (conn/send-bind conn statement params param-oids)]

    (conn/describe-portal conn portal)
    (conn/send-execute conn portal row-count)
    (conn/close-portal conn portal)
    (conn/send-sync conn))

  (res/interact conn :execute))


(defn close-statement [conn ^Map Statement]
  (let [{:keys [statement]}
        Statement]
    (conn/close-statement conn statement))
  (conn/send-sync conn)
  (res/interact conn :close-statement))


(defmacro with-statement
  [[bind conn sql] & body]
  `(let [conn# ~conn
         sql# ~sql
         ~bind (prepare conn# sql#)]
     (try
       ~@body
       (finally
         (close-statement conn# ~bind)))))


(defn authenticate [conn]
  (conn/authenticate conn)
  (res/interact conn :auth)
  conn)


(defn connect [config]
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
