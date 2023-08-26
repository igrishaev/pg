# The Client

<!-- toc -->

- [Basic usage](#basic-usage)
- [Queries](#queries)
- [Execute](#execute)
- [Prepared Statements](#prepared-statements)
- [Processing result with :fn-result](#processing-result-with-fn-result)
- [Column names](#column-names)
- [Column duplicates](#column-duplicates)
- [Reducers and bundles](#reducers-and-bundles)
  * [Java](#java)
  * [Kebab-keys](#kebab-keys)
  * [Matrix](#matrix)
  * [Index by](#index-by)
  * [Group by](#group-by)
  * [Key-value](#key-value)
  * [Custom reducers](#custom-reducers)
- [Transactions](#transactions)
  * [Always Rollback](#always-rollback)
  * [Read-only](#read-only)
  * [Isolation level](#isolation-level)
  * [Manual transactions and status check](#manual-transactions-and-status-check)
- [Configuration](#configuration)
- [Authorization](#authorization)
- [Cloning a connection](#cloning-a-connection)
- [Cancelling a query](#cancelling-a-query)
- [Notices](#notices)
- [Thread safety](#thread-safety)
- [Debugging](#debugging)

<!-- tocstop -->

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

## Column names

By default, the client library turns all the column names to keywords. It
doesn't take any kebab-case transformations into account: a column `"user_name"`
becomes `:user_name`.

~~~clojure
(pg/query conn "select 42 as the_answer")

[{:the_answer 42}]
~~~

An optional parameter `:fn-column` changes this behaviour. It's a function that
takes a string column and returns whatever you want. Here is how you can obtain
upper-cased string keys:

~~~clojure
(require '[clojure.string :as str])

(pg/query conn "select 42 as the_answer" {:fn-column str/upper-case})

[{"THE_ANSWER" 42}]
~~~

Of course, you can pass a complex function that transforms the string somehow
and then turns it into a keyword or a symbol.

Some Clojure programmers prefer kebab-case keywords (which I honestly consider a
bad practice, but still). For this, do one of the two options. Either get such
function from the `pg.client.func` namespace:

~~~clojure
(require '[pg.client.func :as func])

(pg/query conn "select 42 as the_answer" {:fn-column func/kebab-keyword})

[{:the-answer 42}]
~~~

Or pass it as a "bundle":

~~~clojure
(require '[pg.client.as :as as])

(pg/query conn "select 42 as the_answer" {:as as/kebab-keys})

[{:the-answer 42}]
~~~

Bundles are maps of several processing functions which we're going to describe a
bit below.

## Column duplicates

In SQL, that's completely fine when a query returns several columns with the
same name, for example:

~~~
SELECT 1 as val, true as val, 'dunno' as val;

 val | val |  val
-----+-----+-------
   1 | t   | dunno
~~~

But from the client prospective, that's unclear what to do with such a result,
especially when you're dealing with maps.

By default, the client library adds numbers to those columns that have already
been in the result. Briefly, for the example above, you'll get `val`, `val_1`
and `val_2` columns:

~~~clojure
(pg/query conn "SELECT 1 as val, true as val, 'dunno' as val")

[{:val 1, :val_1 true, :val_2 "dunno"}]
~~~

This behaviour stacks with the `fn-column` parameter: the `fn-column` get
applied after the column names have been transformed.

~~~clojure
(pg/query conn "SELECT 1 as val, true as val, 'dunno' as val" {:as as/kebab-keys})

[{:val 1, :val-1 true, :val-2 "dunno"}]
~~~

The function which is responsible for column duplicates processing is called
`:fn-unify`. It takes a vector of strings and must return a vector of something
(strings, keywords, symbols).

## Reducers and bundles

As you're seen before, the result of `pg/query` or `pg/execute` is a vector of
maps. Although it is most likely what you want by default, there are other ways
to obtain the result in another shape.

There is a `pg.client.as` namespaces that carries "bundles": named maps with
predefined parameters, mostly functions. Passing these maps into the optional
`:as` field when querying data affects how the rows will be processed.

### Java

The `as/java` bundle builds an `ArrayList` of mutable `HashMap`s. Both the
top-level set of rows and its children are mutable:

~~~clojure
(def res (pg/query conn "SELECT 42 as the_answer" {:as as/java}))

(.add res :someting)
(.put (.get res 0) :some-key "A")

;; [{:the_answer 42, :some-key "A"} :someting]
~~~

### Kebab-keys

The `kebab-keys` bundle we have already seen in action: it just transforms the
keys from `:foo_bar` to `:foo-bar`:

~~~clojure
(pg/query conn "SELECT 42 as the_answer" {:as as/kebab-keys})

[{:the-answer 42}]
~~~

### Matrix

The `as/matrix` bundle is useful for getting values without names:

~~~clojure
(pg/query conn "SELECT 1, false, 'hello'" {:as as/matrix})

[[1 false "hello"]]
~~~

The result will be just vector of vectors which is convenient for writing to CSV
or Excel files.

### Index by

The following case happens quite often: you select certain entities and then you
need to build an index map by id. Namely, transform this:

~~~clojure
[{:id 1 :name "Ivan"}
 {:id 2 :name "Juan"}
 ...]
~~~

to this:

~~~clojure
{1 {:id 1 :name "Ivan"}
 2 {:id 2 :name "Juan"}
 ...}
~~~

That would be great of course to do that not once *you have read* the rows from
the database but *as you're reading* the rows. That would save the lines of code
and resources.

The `as/index-by` reducer does it for you. It's a function that takes a row
function and returs a bundle:

~~~clojure
(pg/query conn "select * from users" {:as (as/index-by :id)})
p
{1 {:id 1, :name "Ivan", :age 37},
 2 {:id 2, :name "Juan", :age 38}}
~~~

Of course, a function which is passed to `index-by` might be something more
complex than an ordinary keyword. It can be a call of `juxt` if you need to
group rows by several keys:

~~~clojure
(pg/query conn "select * from users" {:as (as/index-by (juxt :id :name))})

{[1 "Ivan"] {:id 1, :name "Ivan", :age 37},
 [2 "Juan"] {:id 2, :name "Juan", :age 38}}
~~~

### Group by

The `as/group-by` reducer acts like the standard `group-by` function. The it
collects a map where a key is a result of `(f row)`, and the value is a vector
of matched rows. The main difference is, it fills the result on the fly as the
data arrives from the server.

Imagine there are more users named Ivan and Juan in our database. Here is how we
can select and group them by name:

~~~clojure
(pg/query conn "select * from users" {:as (as/group-by :name)})

{"Ivan" [{:id 1, :name "Ivan", :age 37}
         {:id 3, :name "Ivan", :age 37}],
 "Juan" [{:id 2, :name "Juan", :age 38}
         {:id 4, :name "Juan", :age 38}]}
~~~

### Key-value

There as a `kv` reducer that allows you to build *any map* you want. It takes
two parameters: a key function (fk) and a value function (fv). The result will
be a map like this:

~~~clojure
{(fk row) (fv row)}
~~~

The `as/kv` reducer is useful when you want to get a map from rows, for example
a mapping from the id to the name:

~~~clojure
(pg/query conn "select * from users" {:as (as/kv :id :name)})

{1 "Ivan", 2 "Juan", 3 "Ivan", 4 "Juan"}
~~~

### Custom reducers

Making a custom reducer means declaring either a map or a function that returns
a map of the following structure:

- `:fn-init`: a function that returns an empty accumulator value;

- `:fn-reduce`: a function that takes the accumulator and a row and adds the row
  to the accumulator;

- `:fn-finalize`: a function that takes the accumulator and returns the final
  value.

Here is an example of the `kv` reducer:

~~~clojure
(defn kv [fk fv]
  {:fn-init #(transient {})
   :fn-reduce (fn [acc! row]
                (assoc! acc! (fk row) (fv row)))
   :fn-finalize persistent!})
~~~

## Transactions

There is a special `pg/with-tx` macro to wrap several expressions in a single
transaction. It takes a connection object and produces BEGIN ... COMMIT
commands:

~~~clojure
(let [sql "INSERT INTO users (name, age) VALUES ($1, $2)"]
  (pg/with-tx [conn]
    (pg/execute conn sql ["Jim" 20])
    (pg/execute conn sql ["Bob" 30])))
~~~

Here is what you would see in the logs:

~~~clojure
BEGIN
INSERT INTO users (name, age) VALUES ($1, $2)
  parameters: $1 = 'Jim', $2 = '20'
INSERT INTO users (name, age) VALUES ($1, $2)
  parameters: $1 = 'Bob', $2 = '30'
COMMIT
~~~

Should an exception pop up the middle, the whole transaction ends up with
ROLLBACK and the exception is re-thrown:

~~~clojure
(let [sql "INSERT INTO users (name, age) VALUES ($1, $2)"]
  (pg/with-tx [conn]
    (pg/execute conn sql ["Jim" 20])
    (* 42 nil) ;; <-
    (pg/execute conn sql ["Bob" 30])))

BEGIN
INSERT INTO users (name, age) VALUES ($1, $2)
  parameters: $1 = 'Jim', $2 = '20'
ROLLBACK

Execution error (NullPointerException) at ...
Cannot invoke "Object.getClass()" because "x" is null
~~~

### Always Rollback

The `pg/with-tx` macro takes additional options for precise control over the
transaction. Passing the `{:rollback? true}` would end up a transaction with
rolling back the changes even if there was no an error.

Here we create a couple of users in a rolling-back transaction. Right after you
exit the `pg/with-tx` macro, all the changes you made get wiped.

~~~clojure
(let [sql "INSERT INTO users (name, age) VALUES ($1, $2)"]
  (pg/with-tx [conn {:rollback? true}]
    (pg/execute conn sql ["Jim" 20])
    (pg/execute conn sql ["Bob" 30])))
~~~

The logs:

~~~clojure
BEGIN
INSERT INTO users (name, age) VALUES ($1, $2)
  parameters: $1 = 'Jim', $2 = '20'
INSERT INTO users (name, age) VALUES ($1, $2)
  parameters: $1 = 'Bob', $2 = '30'
ROLLBACK
~~~

Always-rollback transactions are great for testing: first you insert something
into the database, perform some checks, rollback, and the database stays
untouched. Of course, this is not the case for multi-threaded tests or some
tricky logic when multiple DB connections are involved.

### Read-only

Passing the `{:read-only? true}` map will spawn a read-only transaction where
only SELECT and SHOW commands are available. Triggering INSERT, DELETE, CREATE
and similar commands would make Postgres to respond with an error.

Here we try to create a couple of users in read-only mode:

~~~clojure
(let [sql "INSERT INTO users (name, age) VALUES ($1, $2)"]
  (pg/with-tx [conn {:read-only? true}]
    (pg/query conn "select * from users")
    (pg/execute conn sql ["Bob" 30])))

Execution error (ExceptionInfo) at pg.client.result/finalize-errors! (result.clj:537).
clojure.lang.ExceptionInfo: ErrorResponse
{:error
  {:msg :ErrorResponse,
   :errors {:severity "ERROR",
            :verbosity "ERROR",
            :code "25006",
            :message "cannot execute INSERT in a read-only transaction",
            :file "utility.c",
            :line "414",
            :function "PreventCommandIfReadOnly"}}}
~~~

Usually, the read-only option is mandatory for replicas. Should you try to write
something to the replica by mistake, you'll get an exception.

### Isolation level

The `{:isolation-level ...}` options sets the isolation level for the new
transaction. Here is an example of setting SERIALIZABLE level although there is
no need for that, actually.

~~~clojure
(let [sql "INSERT INTO users (name, age) VALUES ($1, $2)"]
  (pg/with-tx [conn {:isolation-level :serializable}]
    (pg/execute conn sql ["Jim" 20])
    (pg/execute conn sql ["Bob" 30])))
~~~

The logs:

~~~
BEGIN
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE
INSERT INTO users (name, age) VALUES ($1, $2)
  parameters: $1 = 'Jim', $2 = '20'
INSERT INTO users (name, age) VALUES ($1, $2)
  parameters: $1 = 'Bob', $2 = '30'
COMMIT
~~~

The level might be a keyword, a string or a symbol, both in lower or upper
case. For example, `:SERIALIZABLE`, `:serializable`, `"SERIALIZABLE"` and so
on. Those levels that consist from two words, e.g. `REPEATABLE READ`, are
separated with a hyphen: `:repeatable-read`, `"REPEATABLE-READ"`, etc.

Just a reminder, there are four isolations level in Postgres:

- READ UNCOMMITTED
- READ COMMITTED (default)
- REPEATABLE READ
- SERIALIZABLE

**Use them wisely: never set a level explicitly unless you clearly understand
what is the default level and why doesn't it satisfy you.**

### Manual transactions and status check

On the low level, transactions are driven by the `pg/begin`, `pg/commit` and
`pg/rollback` functions. They all take a single argument (a connection) and send
a corresponding command to the server.

Briefly, the `pg/with-tx` macro boils down to the following code:

~~~clojure
(pg/begin conn)
(try
  (pg/execute conn ...)
  (pg/execute conn ...)
  (pg/commit conn)
  (catch Throwable e
    (pg/rollback conn)
    (throw e)))
~~~

Sometimes, you'd like to know the current state of a connection: whether it's in
a transaction or not, or if the transaction has been aborted. The `pg/status`
takes returns a single-character keyword that represents the current state:

~~~clojure
(pg/status conn)
:I
~~~

- `:I` stands for Idle: there is no a transaction;
- `:T` stands for Transaction: the connection is in the middle of a transaction;
- `:E` stands for Error: the transaction has failed. Any subsequent query would
  lead to an error response.

There are also shortcuts to check the state: `pg/idle?`, `pg/in-transaction?`
and `pg/tx-error?`. Here is a quick session:

~~~clojure
;; no transaction

(pg/status conn)
:I

(pg/idle? conn)
true

;; begin transaction

(pg/begin conn)

(pg/status conn)
:T

(pg/in-transaction? conn)
true

;; spoil the transaction

(pg/query conn "selekt ...")

Execution error (ExceptionInfo) at ...
ErrorResponse syntax error at or near \"selekt\"...

(pg/status conn)
:E

(pg/tx-error? conn)
true

;; rolling back

(pg/rollback conn)

(pg/status conn)
:I

(pg/idle? conn)
true
~~~

## Configuration

A config map that you pass to `pg/connect` has various fields. Most of them are
not necessary and derived from the default map. Below, please find a list of
parameters and their defaults.

| Field               | Default                          | Comment                                            |
|---------------------|----------------------------------|----------------------------------------------------|
| `:host`             | `"127.0.0.1"`                    | Host                                               |
| `:port`             | `5432`                           | Port                                               |
| `:database`         | -                                | Database                                           |
| `:user`             | -                                | Username                                           |
| `:password`         | -                                | Password                                           |
| `:protocol-version` | `196608` (declared in constants) | PG protocol version                                |
| `:binary-encode?`   | `false`                          | Use binary protocol to write data                  |
| `:binary-decode?`   | `false`                          | Use binary protocol to read data                   |
| `:fn-notice`        | `pg.client.conn/fn-notice`       | 1-arg function to handle notices (see below)       |
| `:fn-notification`  | `pg.client.conn/fn-notification` | 1-arg function to handle notifications (see below) |
| `:socket`           | (see below)                      | A nested map with socket options                   |

The `:socket` map has the following sub-options:

| Field             | Default | Comment                                                                   |
|-------------------|---------|---------------------------------------------------------------------------|
| `:tcp-no-delay?`  | `true`  | Set `TCP_NODELAY` socket boolean property                                 |
| `:so-keep-alive?` | `true`  | Set `SO_KEEPALIVE` socket boolean property                                |
| `:so-reuse-addr?` | `true`  | Set `SO_REUSEADDR` socket boolean property                                |
| `:so-reuse-port?` | `true`  | Set `SO_REUSEPORT` socket boolean property                                |
| `:so-rcv-buf`     | -       | Set `SO_RCVBUF` socket size; **must be** an integer (e.g. `(int 123456)`) |
| `:so-snd-buf`     | -       | Set `SO_SNDBUF` socket size; **must be** an integer (e.g. `(int 123456)`) |


Here is an example of the configuration:

~~~clojure
{:host "127.0.0.1"
 :port 5432
 :database "project_dev"
 :user "ivan"
 :password "Secret123"
 :fn-notice my-notice-handler
 :fn-notification my-notification-handler
 :protocol-version const/PROTOCOL_VERSION
 :binary-encode? true
 :binary-decode? true
 :socket {:tcp-no-delay? true
          :so-keep-alive? true
          :so-reuse-addr? true
          :so-reuse-port? true
          :so-rcv-buf (int ...)
          :so-snd-buf (int ...)}}
~~~

## Authorization

The PG client supports several authentication pipelines:

| Title          | pg_hba.conf     | Comment                                                                       |
|----------------|-----------------|-------------------------------------------------------------------------------|
| No password    | `trust`         | No password is sent; used when user & host are trusted                        |
| Clear password | `password`      | The password is sent unmasked; quite unsafe                                   |
| MD5            | `md5`           | The password is sent being MD5-hashed with salt; the default method prior v15 |
| SASL           | `scram-sha-256` | 3-steps pipeline with complex algorith; set as default since v15              |

The SASL method includes two algorithms: SCRAM-SHA-256 and
SCRAM-SHA-256-PLUS. It's up toe the client which one to use. At the moment, only
SCRAM-SHA-256 is implemented.

## Cloning a connection

The `pg/clone` function spawns a new connection from an existing one. It reuses
the config from a given connection.

~~~clojure
(pg/with-connection [conn config]
  (with-open [conn2 (pg/clone conn)]
    (pg/query conn2 "select 1 as one")))

[{:one 1}]
~~~

## Cancelling a query

Sometimes, a poorly composed query might hang. If you have a reference to the
connection that as spawned such a query, you may cancel it. Cancelling a query
requires a new connection to be opened and thus is usually done in a separate
thread.

Imagine you're running a long query in a future:

~~~clojure
(def fut
  (future
    (pg/query conn "select pg_sleep(600) as sleep")))
~~~

The `pg/cancel` takes a connection and cancels its current query:

~~~clojure
(pg/cancel conn)
~~~

Now if you try to deref the future, you'll get an exception caused by an error
response from the server:


~~~clojure
@fut

java.util.concurrent.ExecutionException ...
clojure.lang.ExceptionInfo: ErrorResponse ...
~~~

Here is what inside the `ex-data`:

~~~clojure
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
~~~

Under the hood, canceling a query means taking some private data from the target
connection, opening a new connection and sending a special message. Then, the
new connection gets closed immediately.

## Notices

In Postgres, notices are short informative messages shown to the user
sometimes. For example, if you try to `ROLLBACK` although there was no `BEGIN`
first, you won't get not an error but a notice instead.

~~~
ROLLBACK;
WARNING:  there is no transaction in progress
~~~

**Don't mix notices with notifications (see the corresponding chapter).**

By default, the client library just prints the `NoticeResponse` map with all the
fields. You're welcome to pass your own notice handler which logs them, store in
an atom or whatever else:

~~~clojure
(defn my-notice-handler [NoticeResponse]
  (log/infof "Notice response: %s" NoticeResponse))

;; or

(def notices! (atom []))

(defn my-notice-handler [NoticeResponse]
  (swap! notices! conj NoticeResponse))

;; config

{:host "127.0.0.1"
 :port 5432
 ...
 :fn-notice my-notice-handler}
~~~

[notify]: https://www.postgresql.org/docs/current/sql-notify.html


## Thread safety

Not safe, use pool

## Debugging

~~~clojure
pg.debug

PG_DEBUG=1 lein with-profile +test repl

<-  {:msg :StartupMessage, :protocol-version 196608, :user test, :database test, :options nil}
 -> {:msg :AuthenticationSASL, :status 10, :sasl-types #{SCRAM-SHA-256}}
<-  {:msg :SASLInitialResponse, :sasl-type SCRAM-SHA-256, :client-first-message n,,n=test,r=2c0549c8-ef3f-44d4-82e4-4ad99303f7ec}
 -> {:msg :AuthenticationSASLContinue, :status 11, :server-first-message r=2c0549c8-ef3f-44d4-82e4-4ad99303f7echCLyIqVeK1lswQUcIZPZgNB5,s=qkGROo12a/m8jDa9wv90TA==,i=4096}
<-  {:msg :SASLResponse, :client-final-message c=biws,r=2c0549c8-ef3f-44d4-82e4-4ad99303f7echCLyIqVeK1lswQUcIZPZgNB5,p=IbvOGV7fKIj+OIt54dopu/HI35+9Q67R7uPRp1/Ein8=}
 -> {:msg :AuthenticationSASLFinal, :status 12, :server-final-message v=0Mj1tVhYjDUKt7k9ClCkj9h/6zyVtLMAlZ9HIKA6vyc=}
 -> {:msg :AuthenticationOk, :status 0}
 -> {:msg :ParameterStatus, :param in_hot_standby, :value off}
 -> {:msg :ParameterStatus, :param integer_datetimes, :value on}
 -> {:msg :ParameterStatus, :param TimeZone, :value Etc/UTC}
 -> {:msg :ParameterStatus, :param IntervalStyle, :value postgres}
 -> {:msg :ParameterStatus, :param is_superuser, :value on}
 -> {:msg :ParameterStatus, :param application_name, :value }
 -> {:msg :ParameterStatus, :param default_transaction_read_only, :value off}
 -> {:msg :ParameterStatus, :param scram_iterations, :value 4096}
 -> {:msg :ParameterStatus, :param DateStyle, :value ISO, MDY}
 -> {:msg :ParameterStatus, :param standard_conforming_strings, :value on}
 -> {:msg :ParameterStatus, :param session_authorization, :value test}
 -> {:msg :ParameterStatus, :param client_encoding, :value UTF8}
 -> {:msg :ParameterStatus, :param server_version, :value 16beta2 (Debian 16~beta2-1.pgdg120+1)}
 -> {:msg :ParameterStatus, :param server_encoding, :value UTF8}
 -> {:msg :BackendKeyData, :pid 5671, :secret-key -1344960525}
 -> {:msg :ReadyForQuery, :tx-status :I}
<-  {:msg :Query, :query select pg_sleep(60) as sleep}

<-  {:msg :CancelRequest, :code 80877102, :pid 5671, :secret-key -1344960525}
<-  {:msg :Terminate}

 -> {:msg :RowDescription, :column-count 1, :columns [{:index 0, :name sleep, :table-oid 0, :column-oid 0, :type-oid 2278, :type-len 4, :type-mod -1, :format 0}]}
 -> {:msg :ErrorResponse, :errors {:severity ERROR, :verbosity ERROR, :code 57014, :message canceling statement due to user request, :file postgres.c, :line 3396, :function ProcessInterrupts}}
 -> {:msg :ReadyForQuery, :tx-status :I}

ns pg.client.debug
~~~
