(ns repl
  (:require
   [pg.client :as pg]
   #_[pg.pool :as pool]
   ))

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



(defn fn-notice-handler [NoticeResponse]
  (log/infof "Notice response: %s" NoticeResponse))

(def notices! (atom []))

(defn fn-notice-handler [NoticeResponse]
  (swap! notices! conj NoticeResponse))



{:host "127.0.0.1"
 :port 5432
 ...
 :fn-notice fn-notice-handler}


(pg/rollback)


(defn fn-notification [NotificationResponse]
  (log/info NotificationResponse))

(defn fn-notification [NotificationResponse]
  (future
    (process-notification NotificationResponse)))

{:msg :NotificationResponse
 :pid pid
 :channel channel
 :message message}


(def notifications!
  (atom []))

(defn fn-notification [NotificationResponse]
  (swap! notifications! conj NotificationResponse))


(def conn1 (pg/connect {... :fn-notification fn-notification}))
(def conn2 (pg/connect ...))

(pg/query conn1 "listen FOO")

(pg/query conn2 conn "notify FOO, 'kek'")
(pg/query conn2 conn "notify FOO, 'lol'")

@notifications!
...

(pg/query conn2 "unlisten FOO")

further notifications won't work any longer.


(def pg-config
  {:host "127.0.0.1"
   :port 10150
   :user "test"
   :password "test"
   :database "test"})

(def pool-config
  {:min-size 1
   :max-size 4
   :ms-lifetime (* 1000 60 60)})


(def pool
  (pool/make-pool pg-config pool-config))

(pool/with-connection [conn pool]
  (pg/query conn "select 1 as one"))

(pool/with-pool [pool pg-config pg-config]
  (future
    (pool/with-connectoin [conn pool]
      ...))
  (future
    (pool/with-connectoin [conn pool]
      ...)))


(pool/with-pool [pool pg-config pool-config]
  (pool/with-connection [conn pool]
    (pg/query conn "select 1 as one")))


(pool/with-pool [pool pg-config {:min-size 1
                                 :max-size 2}]

  (future
    (pool/with-connection [conn pool]
      (pg/query conn "select pg_sleep(600) as sleep")))

  (future
    (pool/with-connection [conn pool]
      (pg/query conn "select pg_sleep(600) as sleep")))

  (pool/with-connection [conn pool]
    (pg/query conn "select 42")))



(pool/with-pool [pool pg-config {:min-size 1
                                 :max-size 2}]

  (future
    (pool/with-connection [conn pool]
      (pg/query conn "select pg_sleep(600) as sleep")))

  (future
    (pool/with-connection [conn pool]
      (pg/query conn "select pg_sleep(600) as sleep")))

  (Thread/sleep 100)

  (pool/with-connection [conn pool]
    (pg/query conn "select pg_sleep(600) as sleep")))



(require '[com.stuartsierra.component :as component])


(def pool-idle
  (pool/component pg-config))


(def pool-started
  (component/start pool-idle))


(pool/with-connection [conn pool-started]
  (pg/query conn "select 42 as the_answer"))

[{:the_answer 42}]


(def pool-stopped
  (component/stop pool-started))


(defrecord SomeJob [;; opt
                    params
                    ;; deps
                    pool]

  component/Lifecycle

  (start [this]
    (pool/with-connection [conn pool]
      ...))

  (stop [this]
    ...))


(def system
  {:pool
   (pool/component pg-config)

   :some-job
   (-> (map->SomeJob {,,,})
       (component/using [:pool]))})


(def config
  {:host "127.0.0.1"
   :port 10150
   :user "test"
   :password "test"
   :database "test"})

(def conn1
  (pg/connect config))

(def conn2
  (pg/connect config))

(def channel "hello")

(pg/listen conn2 channel)

(pg/notify conn1 channel "test")

(pg/query conn2 "select")

;; PG notification: {:msg :NotificationResponse, :pid 11244, :channel "hello", :message "test"}


(def notifications
  (atom []))

(defn my-handler [notification]
  (swap! notifications conj notification))


(def conn2
  (pg/connect (assoc config :fn-notification my-handler)))

(pg/listen conn2 channel)

(pg/notify conn1 channel "test1")
(pg/notify conn1 channel "test2")

(pg/query conn2 "select")


@notifications

[{:msg :NotificationResponse, :pid 11244, :channel "hello", :message "test1"}
 {:msg :NotificationResponse, :pid 11244, :channel "hello", :message "test2"}]

(defn my-handler [{:keys [pid channel message]}]
  (future
    (some-background-logic ...)))

(pg/unlisten conn2 channel)
