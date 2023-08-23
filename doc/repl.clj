(ns repl
  (:require
   [pg.client :as pg]))

(def config
  {:host "127.0.0.1"
   :port 10150
   :user "test"
   :password "test"
   :database "test"})


(def conn (pg/connect config))

(pg/with-connection [conn config]
  (pg/query conn "select 1 as one"))

(let [conn (pg/connect config)
      data (pg/query conn "select 1 as one")]
  (pg/terminate conn)
  data)

(with-open [conn (pg/connect config)]
  (pg/query conn "select 1 as one"))

(pg/execute "insert into users (name, age) values ($1, $2), ($3, $4) returning id" ["Ivan" 37 "Juan" 38])

(def sql "select * from users where id = $1")

(pg/execute conn sql [1])

(pg/execute conn sql [2])

(let [query "select * from users where id = $1"]
  data1 (pg/execute conn query [1])
  data1 (pg/execute conn query [2])
  {:data1 data1
   :data1 data1})


(pg/query conn "CREATE TABLE users (id serial primary key, name text, age integer)")


(let [sql "INSERT INTO users (name, age) VALUES ($1, $2)"]
  (pg/with-tx [conn]
    (pg/execute conn sql ["Jim" 20])
    (pg/execute conn sql ["Bob" 30])))


(let [sql "INSERT INTO users (name, age) VALUES ($1, $2)"]
  (pg/with-tx [conn]
    (pg/execute conn sql ["Jim" 20])
    (pg/execute conn sql ["Bob" 30])))

(let [sql "INSERT INTO users (name, age) VALUES ($1, $2)"]
  (pg/with-tx [conn {:rollback? true}]
    (pg/execute conn sql ["Jim" 20])
    (pg/execute conn sql ["Bob" 30])))

(let [sql "INSERT INTO users (name, age) VALUES ($1, $2)"]
  (pg/with-tx [conn {:read-only? true}]
    (pg/query conn "select * from users")
    (pg/execute conn sql ["Bob" 30])))




(let [sql "INSERT INTO users (name, age) VALUES ($1, $2)"]
  (pg/with-tx [conn {:isolation-level :serializable}]
    (pg/execute conn sql ["Jim" 20])
    (pg/execute conn sql ["Bob" 30])))


(pg/begin conn)
(pg/execute conn ...)
(pg/commit conn)


(pg/begin conn)
(try
  (pg/execute conn ...)
  (pg/execute conn ...)
  (pg/commit conn)
  (catch Throwable e
    (pg/rollback conn)
    (throw e)))


(pg/with-connection [conn config]
  (with-open [conn2 (pg/clone conn)]
    (pg/query "select 1" conn2)))


(def fut
  (future
    (pg/query conn "select pg_sleep(600) as sleep")))


(pg/cancel conn)

java.util.concurrent.ExecutionException ...
clojure.lang.ExceptionInfo: ErrorResponse ...

(try
  @fut
  (catch ExecutionException e
    (let [data (-> e ex-case ex-data)]
      ...)))

{:error
 {:msg :ErrorResponse,
  :errors
  {:severity "ERROR",
   :verbosity "ERROR",
   :code "57014",
   :message "canceling statement due to user request",
   :file "postgres.c",
   :line "3092",
   :function "ProcessInterrupts"}}}
