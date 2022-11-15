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
           (data/pipeline)))))

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


(defn sync [conn]
  (conn/write-bb conn (msg/make-sync)))


(defn prepare
  ([conn sql]
   (prepare conn sql nil))

  ([conn sql oid-types]

   (let [stmt-name
         (name (gensym "stmt"))

         enc
         (conn/client-encoding conn)

         bb
         (msg/make-parse (codec/str->bytes stmt-name enc)
                         (codec/str->bytes sql enc)
                         oid-types)]
     (conn/with-lock conn
       (-> conn
           (conn/write-bb bb)
           (sync)
           (data/pipeline))
       stmt-name))))


(defn close-statement [conn stmt-name]
  (let [enc
        (conn/client-encoding conn)
        bb
        (msg/make-close-statement
         (codec/str->bytes stmt-name enc))]
    (conn/with-lock conn
      (-> conn
          (conn/write-bb bb)
          (sync)
          (data/pipeline)))))


(defmacro with-statement
  [[bind conn sql & [oid-types]] & body]
  `(let [~bind
         (prepare ~conn ~sql ~oid-types)]
     (try
       ~@body
       (finally
         (close-statement ~conn ~bind)))))


(defn call-statement [conn stmt params]
  (let [

        ])
  )


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


(defn flush []
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

  (terminate -conn)

  (time
   (with-connection [-conn -cfg]
     (dotimes [_ 99999]
       (query -conn "select 1 as one"))))

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
     (dotimes [_ 99999]
       (jdbc/query -db "select 1 as one"))))

  (with-statement [-conn "st2"])


  )
