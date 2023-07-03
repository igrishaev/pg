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

  (let [statement
        (conn/parse conn sql)]

    (conn/describe-statement conn statement)
    (conn/sync conn)

    (res/interact conn #{:ErrorResponse :ReadyForQuery})

    statement))


(defmacro with-statement [conn sql])


(defn execute [conn statement params row-count]

  (let [portal
        (conn/bind conn statement params)]

    (conn/describe-portal conn portal)
    (conn/execute conn portal row-count)
    (conn/sync conn))

  (res/interact conn #{:CommandComplete :EmptyQueryResponse :ErrorResponse :PortalSuspended}))


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

  (def -r (query -conn "select 1 as foo; select 2 as bar"))

  (def -s (prepare -conn "select $1::integer as kek from generate_series(1, 3)"))
  (def -s (prepare -conn ""))

  (def -p (execute -conn -s [1] 1))


  )
