(ns pg.client2.api
  (:require
   [pg.client2.conn :as conn]
   [pg.client2.msg :as msg]
   [pg.client2.result :as res])  )


(defn query [conn sql]
  (let [msg (msg/query query)]
    (conn/send-message conn msg)
    (res/interact conn)))


(defn begin [conn]
  (query conn "BEGIN"))


(defn commit [conn]
  (query conn "COMMIT"))


(defn rollback [conn]
  (query conn "ROLLBACK"))


(defmacro with-tx [conn]
  )


(defn pid [conn]
  (conn/get-pid conn))


(defn prepare [conn sql]
  (let [stmt (name (gensym "statement_"))
        msg (msg/parse stmt sql)]
    (conn/send-message conn msg)
    (res/interact conn #{:ErrorResponse :ReadyForQuery})))


(defmacro with-statement [conn sql])


(defn bind [conn statement params]
  )
