(ns pg.client2.api
  (:require
   [pg.client2.conn :as conn]
   [pg.client2.result :as res])  )


(def ready-or-error
  #{:ReadyForQuery :ErrorResponse})


(defn query [conn sql]
  (conn/query conn sql)
  (res/interact conn
                ready-or-error
                :query))


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

    (res/interact conn
                  ready-or-error
                  :prepare)

    statement))


(defn execute [conn statement params row-count]

  (let [portal
        (conn/bind conn statement params)]

    (conn/describe-portal conn portal)
    (conn/execute conn portal row-count)
    (conn/close-portal conn portal)
    (conn/sync conn))

  (res/interact conn
                ready-or-error
                :execute))


(defn close-statement [conn statement]
  (conn/close-statement conn statement)
  (conn/sync conn)
  (res/interact conn
                ready-or-error
                :close-statement))


(defmacro with-statement
  [[bind conn sql] & body]
  `(let [conn# ~conn
         sql# ~sql
         ~bind (prepare conn# sql#)]
     (try
       ~@body
       (finally
         (close-statement conn# ~bind)))))


(defn authenticate [conn]
  (conn/authenticate conn)
  (res/interact conn
                ready-or-error
                :auth)
  conn)


(defn connect [config]
  (-> config
      (conn/connect)
      (authenticate)))


(defn terminate [conn]
  (conn/terminate conn))


(defmacro with-connection
  [[bind config] & body]
  `(let [~bind (connect ~config)]
     (try
       ~@body
       (finally
         (terminate ~bind)))))


(defn clone [{:as conn :keys [config]}]
  (connect config))


(defn cancel-request

  ([{:as conn :keys [config]}]

   (with-connection [new-conn config]
     (cancel-request new-conn conn)))

  ([conn conn-to-cancel]

   (let [pid
         (conn/get-pid conn-to-cancel)

         secret-key
         (conn/get-secret-key conn-to-cancel)]

     (conn/cancel-request conn pid secret-key))))


#_
(comment

  (def -cfg {:host "localhost"
             :port 15432
             :user "ivan"
             :database "ivan"
             :password "ivan"})

  (with-connection [c -cfg]
    (query c "select 1"))

  (def -conn (connect -cfg))

  (with-statement [stmt -conn "select 1 as num"]
    (execute -conn stmt [] 0))

  (def -r (query -conn "select 1 as foo; select 2 as bar"))

  (def -s (prepare -conn "select $1::integer as kek from generate_series(1, 3)"))
  (def -s (prepare -conn ""))

  (def -p (execute -conn -s [1] 1))


  )
