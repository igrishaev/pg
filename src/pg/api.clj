(ns pg.api
  "
  Public client API.
  "
  (:refer-clojure :exclude [sync flush update])
  (:require
   [pg.conn :as conn]
   [pg.pipeline :as pipeline]

   ))


(defn connect [config]
  (-> config
      conn/connect
      pipeline/auth))


(defn terminate []
  )


(defmacro with-connection []
  )


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

  (def -conn
    (connect
     {:host "127.0.0.1"
      :port 15432
      :user "ivan"
      :database "ivan"
      :password "secret"}))


  )
