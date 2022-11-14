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
   [pg.pipeline.auth :as auth]
   [pg.pipeline.data :as data]))


(defn connect [config]
  (-> config
      conn/connect
      auth/pipeline))


(defn terminate [conn]
  (-> conn
      (conn/write-bb (msg/make-terminate))
      (dissoc :ch :pid :secret-key)))


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


(defn cancell []
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

  (with-connection [-conn -cfg]
    (dotimes [_ 99]
      (println (query -conn "select 1 as one"))))

  (with-statement [-conn "st2"])


  )
