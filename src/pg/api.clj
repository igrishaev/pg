(ns pg.api
  "
  Public client API.
  "
  (:refer-clojure :exclude [sync flush update])
  (:require
   [pg.conn :as conn]
   [pg.msg :as msg]
   [pg.pipeline :as pipeline]

   ))


(defn connect [config]
  (-> config
      conn/connect
      pipeline/auth
      pipeline/init))


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


(defn query []
  )


(defn insert []
  )


(defn insert-batch []
  )


(defn update []
  )


(defn delete []
  )


(defn prepare
  ([conn stmt-name query]
   (prepare conn stmt-name query nil))

  ([conn stmt-name query oid-types]

   )
  )


(defmacro with-transaction []
  )


(defmacro with-statement []
  )



(defn copy-in []
  )


(defn copy-out []
  )


(defn func-call []
  )


(defn notify []
  )


(defn cancell []
  )


(defn close-statement []
  )





(defn get-isolation-level []
  )


(defn set-isolation-level []
  )


(defn sync []
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

  (terminate -conn)

  (with-connection [-conn -cfg]
    (println -conn))


  )
