(ns pg.api
  "
  Public client API.
  "
  (:import pg.stmt.Statement)
  (:refer-clojure :exclude [sync flush update])
  (:require
   [pg.stmt :as stmt]
   [pg.oid :as oid]
   [pg.error :as e]
   [pg.const :as const]
   [pg.codec :as codec]
   [pg.types.encode :as encode]
   [pg.conn :as conn]
   [pg.msg :as msg]
   [pg.quote :as q]
   [pg.pipeline :as pipeline]
   [clojure.string :as str]))


;;
;; Connection
;;

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


(defn flush [conn]
  (conn/write-bb conn (msg/make-flush)))


(defn sync [conn]
  (conn/write-bb conn (msg/make-sync)))


;;
;; Statement
;;

(defn ^Statement prepare-statement
  ([conn sql]
   (prepare-statement conn sql nil))

  ([conn sql oid-params]

   (let [stmt-name
         (name (gensym "stmt-"))

         enc
         (conn/client-encoding conn)

         bb-parse
         (msg/make-parse (codec/str->bytes stmt-name enc)
                         (codec/str->bytes sql enc)
                         oid-params)

         bb-describe
         (msg/make-describe-statement
          (codec/str->bytes stmt-name))

         {:keys [RowDescription
                 ParameterDescription]}
         (-> conn
             (conn/write-bb bb-parse)
             (conn/write-bb bb-describe)
             (sync)
             (pipeline/pipeline))]

     ;; TODO: pass encoding
     ;; TODO: pass query
     (stmt/make-statement stmt-name
                          ParameterDescription
                          RowDescription))))


(defn close-statement [conn ^Statement stmt]
  (let [enc
        (conn/client-encoding conn)
        bb
        (msg/make-close-statement
         (-> stmt
             (stmt/get-name)
             (codec/str->bytes enc)))]

    (-> conn
        (conn/write-bb bb)
        (sync)
        (pipeline/pipeline))

    nil))


(defmacro with-statement
  [[bind conn sql & [oid-types]] & body]
  `(let [~bind
         (prepare-statement ~conn ~sql ~oid-types)]
     (try
       ~@body
       (finally
         (close-statement ~conn ~bind)))))


(defn execute-statement
  [conn ^Statement stmt params & [_out-formats]]
  (let [enc
        (conn/client-encoding conn)

        portal ""
        #_
        (name (gensym "portal-"))

        req-formats [const/FORMAT_BINARY]
        res-formats [const/FORMAT_BINARY]

        enc-opt
        {}

        param-count
        (stmt/param-count stmt)

        req-bytes
        (loop [i 0
               acc []]
          (if (= i param-count)
            acc
            (let [param (get params i)
                  oid (stmt/param-type stmt i)
                  buf (pg.types.encode.binary/mm-encode param oid enc-opt)]
              (recur (inc i) (conj acc buf)))))

        ;; pairs
        ;; (for [param params]
        ;;   (encode/encode param enc))

        ;; in-formats
        ;; (mapv first pairs)

        ;; in-bytes
        ;; (mapv second pairs)

        ;; out-formats
        ;; (cond
        ;;   (int? out-formats)
        ;;   [out-formats]
        ;;   (coll? out-formats)
        ;;   out-formats
        ;;   :else
        ;;   (e/error!
        ;;    "Wrong output format. Must be either an integer or a coll of integers."
        ;;    {:out-formats out-formats}))

        bb-bind
        (msg/make-bind
         (-> portal (codec/str->bytes enc))
         (-> stmt stmt/get-name (codec/str->bytes enc))
         req-formats
         req-bytes
         res-formats)

        bb-exe
        (msg/make-execute
         (codec/str->bytes portal) 0) ;; TODO: amount of rows

        bb-desc
        (msg/make-describe-portal
         (codec/str->bytes portal))]

    (-> conn
        (conn/write-bb bb-bind)
        (conn/write-bb bb-desc)
        (conn/write-bb bb-exe)
        (sync)
        (pipeline/pipeline))))


;;
;; Query
;;

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
   (conn/with-lock conn
     (with-statement [st conn sql oid-types]
       (execute-statement conn st params out-formats)))))


;;
;; Calling a procedure
;;

(defn call-function [conn oid-func & params]

  (let [binary?
        false

        in-formats
        (repeat (count params) const/FORMAT_TEXT)

        bb
        (msg/make-function-call oid-func
                                in-formats

                                )
        ]))


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

  `(conn/with-lock conn

     (begin ~conn)

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
             (print \, (q/quote-text message))))]
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

  (def -conn (connect -cfg))

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
    (println (execute-statement -conn st nil))
    (println (execute-statement -conn st nil)))

  (with-connection [-conn -cfg]
    (with-statement [st -conn "select 1 as one"]
      (println (execute-statement -conn st nil))
      (println (execute-statement -conn st nil))))

  (query -conn
         "select $1 as line1, $1 as line2, now() as time"
         ["hello"]
         [pg.oid/TEXT]
         const/FORMAT_BINARY)

  (query -conn
         "
select
  1::int      as int,
  1::int2     as int2,
  2::int4     as int4,
  3::int8     as int8,

  'hello'          as text1,
  'world'::varchar as text2,

  true        as bool1,
  false       as bool2,

  1.1::float  as float,
  1.2::float4 as float4,
  1.3::float8 as float8,

  1.3::numeric as numeric,

  NULL as null,

  now()                as tsz,
  '2022-01-01'::date   as date,
  '10:10:59'::time     as time,

  '2022-01-01'::timestampz as timestampz,

  'a'::bytea      as bytea

")

  [{:line1 [104, 101, 108, 108, 111],
    :line2 [104, 101, 108, 108, 111],
    :time [0, 2, -112, -86, 35, 5, 83, 97]}]

  )


(defn vassoc [v i x]
  (if (= i (count v))
    (conj (or v []) x)
    (assoc (or v []) i x)))


(defn vassoc-in
  [v [i & is] x]
  (if is
    (vassoc v i (vassoc-in (get v i []) is x))
    (vassoc v i x)))


(defn -parse-int [start ^bytes buf]
  (let [len
        (alength buf)

        out
        (new java.io.ByteArrayOutputStream)]

    (loop [i start]
      (if (= i len)

        [(.toByteArray out) i]

        (let [b (aget buf i)]
          (if (<= 48 b 59)
            (do
              (.write out b)
              (recur (inc i)))
            [(.toByteArray out) i]))))))


(defn -parse-bytes [^long start ^bytes buf pred]
  (let [len
        (alength buf)

        out
        (new java.io.ByteArrayOutputStream)]

    (loop [i start]
      (if (= i len)

        [(.toByteArray out) i]

        (let [b (aget buf i)]
          (if (pred b)
            (do
              (.write out b)
              (recur (inc i)))
            [(.toByteArray out) i]))))))


(defn -pred-int [b]
  (<= 48 b 57))


(defn -pred-bool [b]
  (or (= b 102) (= b 116)))


(defn parse-array [^bytes buf pred]
  (let [len
        (alength buf)

        dims
        (loop [i 0
               result 0]
          (if (= i len)
            result
            (let [b (aget buf i)]
              (if (= b (byte \{))
                (recur (inc i) (inc result))
                result))))]

    (loop [i 0
           path (vec (repeat dims 0))
           acc nil
           item nil
           pos -1]

      (if (= i len)
        acc
        (let [b (aget buf i)]
          (case b

            123 ;; (byte \{)
            (do
              (println \{ path pos item)
              (recur (inc i)
                     path
                     acc
                     item
                     (inc pos)))

            125 ;; (byte \})
            (do
              (println \} path pos item)
              (recur (inc i)
                     (-> path
                         (assoc pos 0))
                     (if item
                       (vassoc-in acc path item)
                       acc)
                     nil
                     (dec pos)))

            44 ;; (byte \,)
            (do
              (println \, path pos item)
              (recur (inc i)
                     (clojure.core/update path pos inc)
                     (if item
                       (vassoc-in acc path item)
                       acc)
                     nil
                     pos))

            ;; else
            (let [[item end]
                  (-parse-bytes i buf pred)]
              (recur (int end) path acc item pos))))))))


#_
(parse-array "{{{1,2,3},{1,2,3}},{{1,2,3},{1,2,3}}}" 3)

#_
(parse-array (.getBytes "{{{1,2,3},{1,2,3}},{{1,2,3},{1,2,3}}}") -pred-int)
