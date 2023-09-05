# Connection Pool

<!-- toc -->

- [Basic usage](#basic-usage)
- [With-pool & with-open](#with-pool--with-open)
- [Config](#config)
- [Thread safety](#thread-safety)
- [Pool Exhausting](#pool-exhausting)
- [Exception handling](#exception-handling)
- [Logs](#logs)
- [Component](#component)

<!-- tocstop -->

To interact with a database effectively, you need a connection pool. A single
connection is fragile on its own: you can easily lose it by an accidental lag in
the network.

Another thing that is worth bearing in mind is, that opening a new connection
every time you want to reach PostgreSQL is expensive. Every connection starts a
new process on the server. If your code opens too many connections, say in a
cycle or within threads/futures, sooner or later you'll reach an error response
saying "too many connections".

The connection pool is an object that holds several open connections at once. It
allows you to *borrow* a connection for some period of time. A borrowed
connection can be only used in a block of code that has borrowed it but nowhere
else. Once the block of code has done with its duties, the connection gets
returned to the pool.

The pool is also capable of calculating the lifetime of connections and their
expiration moments. Once a connection has expired, it gets terminated and the
pool spawns a new connection.

The connection pool is shipped in a dedicated library
`com.github.igrishaev/pg-pool` as it depends on the logging facility.

## Basic usage

Everything related to the pool is located in the `pg.pool` namespace. To run a
pool, at least you need a Postgres config which is passed as the first argument
to the `make-pool` function. This config is used to spawn new connections. The
second map controls the inner pool logic and might be skipped as it has
defaults.

~~~clojure
(ns repl
  (:require
   [pg.client :as pg]
   [pg.pool :as pool]))

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

(println pool)
;; < PG pool, min: 1, max: 4, free: 1, used: 0, lifetime: 3600000 ms >
~~~

Once you've created a pool, borrow a connection using the `with-connection`
macro. The connection will be bound to the first `conn` symbol:

~~~clojure
(pool/with-connection [conn pool]
  (pg/query conn "select 1 as one"))

;; [{:one 1}]
~~~

The rest of the logic depends on the client library but not the pool. Being
inside the `with-connection` macro, you can call `pg/query`, `pg/execute` and
other client API.

Exiting the macro body will put the connection back keeping it open. The
connection is put at the end of the queue and taken from the head. Say, if you
have four connections in the pool, you'll face the same connection each fourth
`with-connection` call.

Once you've done with the pool, terminate it by calling
`pool/terminate`. Terminating a pool closes all the open connections **including
those that are borrowed at the moment**.

~~~clojure
(pool/terminate pool)
~~~

## With-pool & with-open

To open a pool temporarily, use the `with-pool` macro. It wraps a block of code
with logic that opens and terminates a pool. The first argument is a symbol that
a new pool is bound to.

~~~clojure
(pool/with-pool [pool pg-config pool-config]
  (pool/with-connection [conn pool]
    (pg/query conn "select 1 as one")))

;; [{:one 1}]
~~~

Since the `Pool` object implements the `Closeable` interface, it can be used
within the `with-open` macro:

~~~clojure
(with-open [pool (pool/make-pool pg-config)]
  (pool/with-connection [conn pool]
    ...))
~~~

## Config

| Parameter      | Default            | Comment                                                                    |
|----------------|--------------------|----------------------------------------------------------------------------|
| `:min-size`    | 2                  | The minimum number of connections to be opened during the initialization.  |
| `:max-size`    | 8                  | The maximum number of connections to be opened during the initialization.  |
| `:ms-lifetime` | 3.600.000 (1 hour) | The number of milliseconds in which a connection is considered expired. |

Example:

~~~clojure
(def pool-config
  {:min-size 1
   :max-size 4
   :ms-lifetime (* 1000 60 15)}) ;; 15 minutes
~~~

## Thread safety

Unlike a connection object, the pool *is thread-safe* indeed. What it means is,
that two parallel threads cannot obtain the same connection at once. This logic
is serialized: each thread will obtain its own connection one after another.

## Pool Exhausting

Should you exhaust the pool -- meaning you try to borrow a connection while
there aren't any idle connects and the `max-size` of busy connections is reached
-- the pool will throw an exception to let you know about this. Here is a simple
way to trigger this behavior:

~~~clojure
;; create a pool with up to 2 connections
(pool/with-pool [pool pg-config {:min-size 1
                                 :max-size 2}]

  ;; spawn two hanging queries that lock connections
  (future
    (pool/with-connection [conn pool]
      (pg/query conn "select pg_sleep(600) as sleep")))
  (future
    (pool/with-connection [conn pool]
      (pg/query conn "select pg_sleep(600) as sleep")))

  ;; wait for a bit to let the futures start their work
  (Thread/sleep 100)

  ;; try to get a new connection => exception
  (pool/with-connection [conn pool]
    (pg/query conn "select pg_sleep(600) as sleep")))

;; Execution error (ExceptionInfo) at pg.pool/-borrow-connection (pool.clj:95).
;; pool is exhausted: 2 connections in use
~~~

At the moment, there is no way to block the execution and wait until there is a
free connection again. Although it's possible with a blocking version of
`ArrayDeque` class.

## Exception handling

Should any exception occur in the middle of the `with-connection` macro, the DB
connection gets terminated. A new connection is spawned to substitute it in the
pool.

## Logs

The pool logs its events using the `clojure.tools.logging` library. The
namespace is `pg.pool`. Here is an example of logs you would get:

~~~
16:20:35 DEBUG pg.pool - a new connection created: pg10177
16:20:35 DEBUG pg.pool - connection pg10177 has been acquired
16:20:35 DEBUG pg.pool - a new connection created: pg10178
16:20:35 DEBUG pg.pool - connection pg10177 has been acquired
...
16:20:35 DEBUG pg.pool - connection pg10177 has been released
16:20:35 DEBUG pg.pool - terminating the pool...
16:20:35 DEBUG pg.pool - terminating connection pg10178
16:20:35 DEBUG pg.pool - terminating connection pg10177
16:20:35 DEBUG pg.pool - pool termination done
~~~

A snippet for Logback configuration: add it into your `logback.xml` file:

~~~xml
<logger name="pg.pool" level="DEBUG" additivity="false">
</logger>
~~~

## Component

[component]:https://github.com/stuartsierra/component

The pool may act as a Component. The `pool/component` function takes both config
maps (pg- and pool-) and returns an object that implements both `start` and
`stop` methods from the `Lifecycle` protocol. The object doesn't open
connections on creation; it waits until you start it.

Here is a quick example where we initiate and start a component manually:

~~~clojure
(require
  '[com.stuartsierra.component :as component])

;; create an idle pool component
(def pool-idle
  (pool/component pg-config))

;; start it
(def pool-started
  (component/start pool-idle))

;; use it
(pool/with-connection [conn pool-started]
  (pg/query conn "select 42 as the_answer"))

[{:the_answer 42}]


;; stop it
(def pool-stopped
  (component/stop pool-started))
~~~

Although it works, it's better to use components within a system. Below, is an
example of a component that depends on a pool. Both `:some-job` and `:pool` form
a system. When started, the Job component receives a pool with pre-allocated
connections. Then we reach the database using the `with-connection` macro.

~~~clojure
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
   (-> (map->SomeJob {...})
       (component/using [:pool]))})
~~~
