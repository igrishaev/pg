(ns pg.client2.api
  (:require
   [pg.client2.conn :as conn]
   [pg.client2.result :as res])  )



(defn query [conn sql]
  (conn/query conn sql)
  (res/interact conn #{:ErrorResponse :ReadyForQuery}))


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
        msg 42

        #_
        (msg/parse stmt sql)]
    (conn/send-message conn msg)
    (res/interact conn #{:ErrorResponse :ReadyForQuery})))


(defmacro with-statement [conn sql])


(defn bind [conn statement params]
  )


(defn authenticate [conn]
  (conn/authenticate conn)
  (res/interact conn #{:AuthenticationOk :ErrorResponse})
  conn)


(defn initiate [conn]
  (res/interact conn #{:ReadyForQuery :ErrorResponse})
  conn)


(defn connect [config]
  (-> config
      (conn/connect)
      (authenticate)
      (initiate)))


#_
(comment

  (def -cfg {:host "localhost"
             :port 15432
             :user "ivan"
             :database "ivan"
             :password "ivan"})

  (def -conn (connect -cfg))




  )
