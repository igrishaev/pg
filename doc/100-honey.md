# HoneySQL support

<!-- toc -->

- [About](#about)
- [Installation](#installation)
- [Usage](#usage)

<!-- tocstop -->

## About

[honeysql]: https://github.com/seancorfield/honeysql

[HoneySQL][honeysql] is a well-known library for building SQL expressions from
Clojure maps. It's convenient for making complex queries, for example, when you
have optional JOIN operators. Or you're unaware of the final WHERE conditions as
the filtering parameters come from the request. HoneySQL frees you from building
raw SQL queries by concatenating strings, which is unsafe and leads to SQL
injections.

The `pg-honey` is a small wrapper on top of HoneySQL. It provides special
versions of `query` and `execute` functions that accept not a SQL string but
Clojure maps. The maps get transformed into SQL under the hood and get executed.

## Installation

Install the `pg-honey` package as follows.

Lein:

~~~clojure
[com.github.igrishaev/pg-honey "0.1.10"]
~~~

Deps:

~~~clojure
{com.github.igrishaev/pg-honey {:mvn/version "0.1.10"}}
~~~

## Usage

Import the library:

~~~clojure
(require '[pg.honey :as pgh])
~~~

The `query` function accepts a connection object, a Clojure map representing a
query and a map of options.

~~~clojure
(pgh/query
  conn
  {:select [:*] :from [:users]}
  {:pretty true
   :as as/first})
~~~

The third parameter combines HoneySQL parameters and the standard `query`
options. In the example above, we passed a custom reducer into the `:as`
parameter, and we also specified the `:pretty` HoneySQL option to true. With the
pretty flag enabled, HoneySQL produces a formatted SQL expression, which is
easier to read in logs.

Please note: since the `query` function doesn't allow you to pass any
parameters, the following example will lead to an error response:

~~~clojure
(pgh/query
  conn
  {:select [:*] :from [:users] :where [:= :id 42]}
  {:pretty true
   :as as/first})
~~~

This is a limitation of the PostgreSQL wire protocol: the `Query` message bears
only a pure SQL expression with no parameters. For parameters, use the `execute`
function described below.

The `execute` function acts the same but accepts a Clojure map that might have
values that become parameters when rendering the map. Here is an example:

~~~clojure
(pgh/execute
  conn
  {:select [:*] :from [:users] :where [:= :id 42]}
  {:pretty true
   :as as/first})
~~~

Or:

~~~clojure
(pgh/execute
  conn
  {:insert-into :demo
   :values [{:id 1 :title "test1"}
            {:id 2 :title "test2"}
            {:id 3 :title "test3"}]}
  {:pretty true})
~~~

Under the hood, the `{:inset-into ...}` map gets rendered into a SQL vector:

~~~clojure
["insert into ... values ($1, $2), ($3, $4), ($5, $6)"
 1 "test1" 2 "test2" 3 "test3"]
 ~~~

It gets split on the SQL expression and the parameters, which are passed into
the underlying `pg.client/execute` function.

You can use named parameters that HoneySQL does support. Place a specific
keyword into the `[:param ...]` vector, and pass a map of params into the
options as follows:

~~~clojure
(pgh/execute conn
             {:select [:id :title]
              :from [:demo]
              :where [:and
                      [:= :id 2]
                      [:= :title [:param :title]]]}
             {:pretty true
              :params {:title "test2"}})
~~~

To familiarise yourself with HoneySQL features, please refer to the [official
documentation][honeysql].
