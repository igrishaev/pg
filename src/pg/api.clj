(ns pg.api
  "
  Public client API.
  "
  (:refer-clojure :exclude [sync flush update])
  (:require
   [pg.const :as const]
   [pg.codec :as codec]
   [pg.conn :as conn]
   [pg.msg :as msg]
   [pg.pipeline :as pipeline]))


(defn flush [conn]
  (conn/write-bb conn (msg/make-flush)))


(defn sync [conn]
  (conn/write-bb conn (msg/make-sync)))


(defn connect [config]
  (let [{:keys [user
                database]}
        config

        conn
        (conn/connect config)

        bb
        (msg/make-startup database user const/PROT-VER-14)]

    (conn/write-bb conn bb)
    (pipeline/pipeline conn {:phase :auth})
    conn))


(defn terminate [conn]
  (conn/write-bb conn (msg/make-terminate)))


(defmacro with-connection
  [[bind config] & body]
  `(let [~bind (connect ~config)]
     (try
       ~@body
       (finally
         (terminate ~bind)))))


(defn query-with-params
  [conn sql params]

  (let []

    (conn/with-lock conn
      ()
      )


    )



  )


(defn query

  ([conn sql]
   (let [enc
         (conn/client-encoding conn)

         bb
         (-> sql
             (codec/str->bytes enc)
             (msg/make-query))]

     (conn/with-lock conn
       (-> conn
           (conn/write-bb bb)
           (pipeline/pipeline)))))

  ([conn sql & params]
   (query-with-params conn sql params)))


(defn insert []
  )


(defn insert-batch []
  )


(defn update []
  )


(defn delete []
  )


(defn prepare-statement
  ([conn sql]
   (prepare conn sql nil))

  ([conn sql oid-params]

   (let [stmt-name
         (name (gensym "stmt-"))

         enc
         (conn/client-encoding conn)

         bb
         (msg/make-parse (codec/str->bytes stmt-name enc)
                         (codec/str->bytes sql enc)
                         oid-params)]

     (conn/with-lock conn
       (-> conn
           (conn/write-bb bb)
           (sync)
           (pipeline/pipeline)))

     stmt-name)))


(defn close-statement [conn stmt-name]
  (let [enc
        (conn/client-encoding conn)
        bb
        (msg/make-close-statement
         (codec/str->bytes stmt-name enc))]
    (conn/with-lock conn
      (conn/with-lock conn
        (-> conn
            (conn/write-bb bb)
            (sync)
            (pipeline/pipeline))))
    nil))


(defmacro with-statement
  [[bind conn sql & [oid-types]] & body]
  `(let [~bind
         (prepare ~conn ~sql ~oid-types)]
     (try
       ~@body
       (finally
         (close-statement ~conn ~bind)))))


(defn execute-statement [conn stmt params]
  (let [enc
        (conn/client-encoding conn)

        portal
        (name (gensym "portal-"))

        params-encoded []
        #_
        (for [param params]
          (encode param))

        bb-bind
        (msg/make-bind
         (codec/str->bytes portal enc)
         (codec/str->bytes stmt enc)
         []
         []
         [const/FORMAT_TEXT])

        bb-exe
        (msg/make-execute
         (codec/str->bytes portal) 0)

        bb-desc
        (msg/make-describe-portal
         (codec/str->bytes portal))]

    (conn/with-lock conn
      (-> conn
          (conn/write-bb bb-bind)
          (conn/write-bb bb-desc)
          (conn/write-bb bb-exe)
          (sync)
          (pipeline/pipeline)))))


;;
;; Transactions
;;


(defn begin [conn]
  (query conn "BEGIN"))


(defn commit [conn]
  (query conn "COMMIT"))


(defn rollback [conn]
  (query conn "ROLLBACK"))


(defn get-isolation-level [conn]
  (-> conn
      (query "SHOW TRANSACTION ISOLATION LEVEL")
      first
      :transaction_isolation))


;; TODO: parse level
(defn set-isolation-level [conn level]
  (let [sql
        (format "SET TRANSACTION ISOLATION LEVEL %s" level)]
    (query conn sql)))


(defmacro with-transaction
  [[conn & [iso-level]] & body]

  `(do

     ~(when iso-level
        `(when ~iso-level
           (set-isolation-level ~conn ~iso-level)))

     (try
       (let [result# (do ~@body)]
         (commit ~conn)
         result#)

       (catch Throwable e#
         (rollback ~conn)
         (throw e#)))))


(defn copy-in []
  )


(defn copy-out []
  )


(defn call-function [conn oid-func & params]

  (let [binary?
        false

        in-formats
        (repeat (count params) const/FORMAT_TEXT)

        bb
        (msg/make-function-call oid-func
                                in-formats

                                )
        ])



  )


(defn notify []
  )


(defn cancell-query []
  )


(defn reducible-query []
  )


(defn get-by-id []
  )


(defn find-by-keys []
  )


(defn find-one-by-keys []
  )


(defn component []
  )


(defn print-notice-handler
  [conn messages]
  (println "Server notice:")
  (doseq [{:keys [type message]} messages]
    (println " -" type message)))


#_
(comment

  (def -cfg
    {:host "127.0.0.1"
     :port 15432
     :user "ivan"
     :database "ivan"
     :password "secret"
     :fn-notice-handler print-notice-handler})

  (def -conn
    (connect -cfg))

  (query -conn "select 1 as one")

  (def -st
    (prepare -conn
             "select now() as now, 'foo' as text"
             [pg.oid/TIMESTAMP, pg.oid/TEXT]))

  (execute -conn -st nil)

  (terminate -conn)

  (time
   (with-connection [-conn -cfg]
     (dotimes [_ 9999]
       (query -conn "select date, kv from log1v"))))

  (time
   (dotimes [_ 9999]
     (query -conn "select date, kv from log1v")))

  (require
   '[clojure.java.jdbc :as jdbc])

  (with-transaction [-conn]
    (query -conn "select 2")
    (query -conn "select 1"))

  (def -spec
    {:dbtype "postgresql"
     :port 15432
     :dbname "ivan"
     :host "127.0.0.1"
     :user "ivan"
     :password "secret"})

  (time
   (jdbc/with-db-connection [-db -spec]
     (dotimes [_ 999]
       (jdbc/query -db "select date, kv from log1v"))))

  (with-statement [st -conn "select 1 as one"]
    (println (execute -conn st nil))
    (println (execute -conn st nil))
    )

  (with-connection [-conn -cfg]
    (with-statement [st -conn "select 1 as one"]
      (println (execute -conn st nil))
      (println (execute -conn st nil))))


  )
