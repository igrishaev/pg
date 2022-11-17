(ns pg.api
  "
  Public client API.
  "
  (:refer-clojure :exclude [sync flush update])
  (:require
   [pg.error :as e]
   [pg.const :as const]
   [pg.codec :as codec]
   [pg.types.encode :as encode]
   [pg.conn :as conn]
   [pg.msg :as msg]
   [pg.quote :as q]
   [pg.pipeline :as pipeline]
   [clojure.string :as str]))


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

  ([conn sql params]
   (query conn sql params nil))

  ([conn sql params oid-types]
   (query conn sql params oid-types const/FORMAT_TEXT))

  ([conn sql params oid-types out-formats]
   (with-statement [st conn sql oid-types]
     (execute-statement conn st params out-formats))))


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


(defmacro with-statement
  [[bind conn sql & [oid-types]] & body]
  `(let [~bind
         (prepare ~conn ~sql ~oid-types)]
     (try
       ~@body
       (finally
         (close-statement ~conn ~bind)))))


(defn execute-statement
  [conn stmt params out-formats]
  (let [enc
        (conn/client-encoding conn)

        portal
        "" #_(name (gensym "portal-"))

        pairs
        (for [param params]
          (encode/encode param enc))

        in-formats
        (mapv first pairs)

        in-bytes
        (mapv second pairs)

        out-formats
        (cond
          (int? out-formats)
          [out-formats]
          (coll? out-formats)
          out-formats
          :else
          (e/error!
           "Wrong output format. Must be either an integer or a coll of integers."
           {:out-formats out-formats}))

        bb-bind
        (msg/make-bind
         (codec/str->bytes portal enc)
         (codec/str->bytes stmt enc)
         in-formats
         in-bytes
         out-formats)

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


(defn- -parse-iso-level [level]
  (-> level name (str/replace #"-|_" " ")))


(defn set-isolation-level [conn level]
  (let [sql
        (format "SET TRANSACTION ISOLATION LEVEL %s"
                (-parse-iso-level level))]
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


;;
;; Copy in & out
;;

(defn copy-in []
  )


(defn copy-out []
  )


;;
;; Listen & Notify
;;

(defn- -quote-str [string]
  (str \' (str/replace string #"'" "''") \'))


(defn listen [conn channel]
  (let [sql
        (format "listen %s" (name channel))]
    (query conn sql)))


(defn unlisten [conn channel]
  (let [sql
        (format "unlisten %s" (name channel))]
    (query conn sql)))


(defn notify
  ([conn channel]
   (notify conn channel nil))

  ([conn channel message]
   (let [sql
         (with-out-str
           (print "notify" (name channel))
           (when message
             (print \, (q/quote-str message))))]
     (query conn sql))))


;;
;; Helpers & wrappers
;;

(defn reducible-query []
  )


(defn get-by-id []
  )


(defn find-by-keys []
  )


(defn find-one-by-keys []
  )


(defn insert []
  )


(defn insert-batch []
  )


(defn update []
  )


(defn delete []
  )


;;
;; Component
;;

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

  (query -conn
         "select $1 as line1, $1 as line2, now() as time"
         ["hello"]
         [pg.oid/TEXT]
         const/FORMAT_BINARY)

  [{:line1 [104, 101, 108, 108, 111],
    :line2 [104, 101, 108, 108, 111],
    :time [0, 2, -112, -86, 35, 5, 83, 97]}]

  )
