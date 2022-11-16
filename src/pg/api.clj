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


(defn prepare
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


(defn execute [conn stmt params]
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
          #_(flush)
          (sync)
          (pipeline/pipeline)))))


(defmacro with-transaction []
  )


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

(defn get-isolation-level []
  )


(defn set-isolation-level []
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


#_
(comment

  (def -cfg
    {:host "127.0.0.1"
     :port 15432
     :user "ivan"
     :database "ivan"
     :password "secret"})

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

  (with-statement [-conn "st2"])


  )
