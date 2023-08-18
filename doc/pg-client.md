# The Client

## Basic usage

Here is a brief example of using the client library:

~~~clojure
(ns scratch
  (:require
   [pg.client :as pg]))

(def config
  {:host "127.0.0.1"
   :port 5432
   :user "test"
   :password "test"
   :database "test"})

(pg/with-connection [conn config]
  (pg/query conn "select 1 as one"))

;; [{:one 1}]
~~~

First, you import the `pg.client` namespace which brings the top-level API
functions to interact with Postgres. The `config` map above specifies the
minimal configuration; it might have more fields which we will explore in a
separate section.

The `with-connection` macro establishes a new connection, binds it to the `conn`
symbol and executes the body. The connection is closed afterwards, even if an
exception pops up.

Technically you can open and terminate a connection manually like this:

~~~clojure
(let [conn (pg/connect config)
      data (pg/query conn "select 1 as one")]
  (pg/terminate conn)
  data)

;; [{:one 1}]
~~~

but it's not recommended.

## Query

## Statements

## Transactions

## Configuration

## Authorization

## Cloning a connection

## Cancelling

## Notifications

## Notices

## Thread safety

## Debugging
