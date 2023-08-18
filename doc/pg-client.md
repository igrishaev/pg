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

but it's not recommended. Also, since the `Connection` object implements
`java.io.Closeable`, it's possible to use in `with-open`:

~~~clojure
(with-open [conn (pg/connect config)]
  (pg/query conn "select 1 as one"))

;; [{:one 1}]
~~~

## Queries

The `pg/query` function above runs a query using a *Simple Wire* protocol
(Postgres has two kind of protocols to communicate each having their own pros
and cons). The function takes a connection object, a string query and a map of
non-required options:

~~~clojure
(pg/query conn "select 1 as one")
(pg/query conn "select 2 as two" {...})
~~~

*Pay attention:* there is no way to pass parameters because the Simple Wire
protocol doesn't support them! In other words, `pg/query` doesn't allow you to
write something like this:

~~~clojure
(pg/query conn "select * from users where id = $1" [42])
~~~

This behaviour is hold by `pg/execute` and statements (see below).

What is the benefit of Simple Wire queries then? They allow you to send multiple
expressions in one separating them with a semicolon:

~~~clojure
(pg/query conn
   "select s.x from generate_series(1, 5) as s(x);
    select s.x from generate_series('2008-03-01 00:00'::timestamp,
                                    '2008-03-04 12:00',
                                    '10 hours') as s(x)")

[[{:x 1} {:x 2} {:x 3} {:x 4} {:x 5}]
 [{:x #object[j.t.LocalDateTime "2008-03-01T00:00"]}
  {:x #object[j.t.LocalDateTime "2008-03-01T10:00"]}
  {:x #object[j.t.LocalDateTime "2008-03-01T20:00"]}
  {:x #object[j.t.LocalDateTime "2008-03-02T06:00"]}
  {:x #object[j.t.LocalDateTime "2008-03-02T16:00"]}
  {:x #object[j.t.LocalDateTime "2008-03-03T02:00"]}
  {:x #object[j.t.LocalDateTime "2008-03-03T12:00"]}
  {:x #object[j.t.LocalDateTime "2008-03-03T22:00"]}
  {:x #object[j.t.LocalDateTime "2008-03-04T08:00"]}]]
~~~

The result will be a vector of two results, each for the corresponding
expression. Each expression has it's own column set and doesn't depend on other
expressions.

Obviously, it's better to avoid mixing DDL expressions like `CREATE TABLE` with
data selection.

## Execute

The `pg/execute` function implements the *Extended Wire* protocol for
Postgres. Although it doesn't allow you to pass multiple expressions separated
with a semicolon, it supports passing parameters.

First, let's prepare a table:

~~~clojure
(pg/query conn "CREATE TABLE users (id serial primary key, name text, age integer)")
~~~

Let's insert a couple of users:

~~~clojure
(pg/execute conn
            "INSERT INTO users (name, age)
             VALUES ($1, $2), ($3, $4)
             RETURNING id"
            ["Ivan" 37 "Juan" 38])

;; [{:id 1} {:id 2}]
~~~

Above, "Ivan" becomes `$1`, 37 becomes `$2` and so on. Now that we have some
data, let's query users by id:

~~~clojure
(def sql "select * from users where id = $1")

(pg/execute conn sql [1])
;; [{:id 1, :name "Ivan", :age 37}]

(pg/execute conn sql [2])
;; [{:id 2, :name "Juan", :age 38}]
~~~

This technics allows to you share the same queries for different values.

## Prepared Statements

The `pg/execute` function above does several things under the hood. It prepares
a statement, binds the parameters to it and obtains a *portal*; then it reads
the from the portal and closes it.

There is a couple of functions to do the same in breakdown. The
`pg/prepare-statement` accepts a connection, a SQL expression and returns a
prepared statement:

~~~clojure
(def stmt (pg/prepare-statement conn sql))
~~~

A prepared statement is just a plain map that carries brief information about
its name (auto-generated), columns and parameters. In the example below, the
statement is called `statement_10637`, there are three columns called "id",
"name" and "age" of type OIDs 23, 25 and 23. There is a single parameter with
type 23 (integer):

~~~clojure
{:statement "statement_10637"
 :RowDescription
 {:msg :RowDescription
  :column-count 3
  :columns
  [{:index 0
    :name "id"
    :table-oid 18024
    :column-oid 1
    :type-oid 23
    :type-len 4
    :type-mod -1
    :format 0}
   {:index 1
    :name "name"
    :table-oid 18024
    :column-oid 2
    :type-oid 25
    :type-len -1
    :format 0}
   {:index 2
    :name "age"
    :table-oid 18024
    :column-oid 3
    :type-oid 23
    :type-len 4
    :type-mod -1
    :format 0}]}
 :ParameterDescription
 {:msg :ParameterDescription
  :param-count 1
  :param-oids [23]}}
~~~

Having a prepared statement, execute it as follows:

~~~clojure
(pg/execute-statement conn stmt [1])
;; [{:id 1, :name "Ivan", :age 37}]

(pg/execute-statement conn stmt [2])
;; [{:id 2, :name "Juan", :age 38}]
~~~

*Pay attention that prepared statements are always bound to a certain
connection. You cannot prepare a statement in one connection and execute it with
another: it will cause an error response from Postgres.*

Once you've done with a prepared statement, close it:

~~~clojure
(pg/close-statement conn stmt)
~~~

Closing statements is important for server as it releases resources allocated to
those statements. To prevent hanging statements, there is a macro called
`with-statement` which closes a statement afterwards:

~~~clojure
(pg/with-statement [stmt conn "INSERT INTO users (name, age) VALUES ($1, $2)"]
  (pg/execute-statement conn stmt ["Petr" 42])
  (pg/execute-statement conn stmt ["Simon" 33]))
~~~

## Processing result with :fn-result

Often, you want to process the result somehow. Say, to take only the first row
of selection (when you know for sure there is either zero or one record).

All the `query`, `execute`, and `execute-statement` accept optional parameters
for such purpose. The `:fn-result` function is applied to the whole result;
usually, you pass `first`:

~~~clojure
(def sql "select * from users where id = $1")

(pg/with-statement [stmt conn sql]
    (let [user1 (pg/execute-statement conn stmt [1] {:fn-result first})
          user2 (pg/execute-statement conn stmt [2] {:fn-result first})]
      {:user1 user1
       :user2 user2}))

{:user1 {:id 1, :name "Ivan", :age 37},
 :user2 {:id 2, :name "Juan", :age 38}}
~~~

Pay attention: when passing the `:fn-result` function to `pg/query` with
multiple expressions, the function is applied to each expression:

~~~clojure
(pg/query conn
          "
           select x from generate_series(0, 3) as s(x);
           select y from generate_series(2, 5) as s(y)
          "
          {:fn-result set})

[#{{:x 1} {:x 3} {:x 2} {:x 0}}
 #{{:y 4} {:y 3} {:y 2} {:y 5}}]
~~~

## Row keys coercion

## Reducers (extended coercion)

## Transactions

## Configuration

## Authorization

## Cloning a connection

## Cancelling

## Notifications

## Notices

## Thread safety

## Debugging
