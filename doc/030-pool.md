# Connection Pool

<!-- toc -->

- [Basic usage](#basic-usage)
- [With-pool macro](#with-pool-macro)
- [Config](#config)
- [Pool Info](#pool-info)
- [Thread safety](#thread-safety)
- [Pool Exhausting](#pool-exhausting)
- [Exception handling](#exception-handling)
- [Logs](#logs)
- [Component](#component)

<!-- tocstop -->

To interact with a database effectively, you need a connection pool. A single
connection is a fragile thing on its own: you can easily lose it by an
accidental lag in the network.

Another thing which is worth bearing in mind is, opening a new connection every
time you want to reach PostgreSQL is expensive. Every connection starts a new
process on the server. If you have a cycle in your code that opens too many
connections or threads/futures that do the same, sooner or later you'll reach an
error response saying "too many connections".

The connection pool is an objects that holds several open connections at
once. It allows you to *borrow* a connection for some period of time. A borrowed
connection can be only used in a block of code that has borrowed it but nowhere
else. Once the block of code has done its duties, the connection gets returned
to the pool.

The pool is also capable of calculating the lifetime of connections and their
expiration moments. Once a connection has expired, it gets terminated and the
pool spawns a new connection.

The connection pool is shipped in a dedicated library as it depends on logging
facility.

## Basic usage

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

(pool/with-connection [conn pool]
  (pg/query conn "select 1 as one"))
;; [{:one 1}]

(pool/terminate pool)
~~~

## With-pool macro

with-open

## Config

~~~clojure
(def pool-defaults
  {:min-size 2
   :max-size 8
   :ms-lifetime (* 1000 60 60 1)})
~~~

## Pool Info

## Thread safety

## Pool Exhausting

## Exception handling

## Logs

## Component
