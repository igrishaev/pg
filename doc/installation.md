# Installation

## General Notes

Although PG is a set of modules, they all have the same version when deployed to
Clojars. For example:

Lein:

~~~clojure
[com.github.igrishaev/pg-client "0.1.1"]
[com.github.igrishaev/pg-pool "0.1.1"]
~~~

Deps:

~~~clojure
{com.github.igrishaev/pg-client {:mvn/version "0.1.1"}
~~~

Lein users may specify a global `pg-version` variable on top of the
`project.clj` and reference it as follows:

~~~clojure
;; project.clj
(def pg-version "0.1.1")

(defproject ...
  :dependencies
  [...
   [com.github.igrishaev/pg-client ~pg-version]
   [com.github.igrishaev/pg-pool ~pg-version]])
~~~

Usually, you don't need to specify more than one package because they depend on
each other and will be download automatically.

## Installing the Client (without a pool)

The `pg-client` module ships a client access to Postgres. Since the coneection
pool depends on logging facility, it's shipped in a separate package.

~~~clojure
[com.github.igrishaev/pg-client "0.1.1"]
~~~

Deps:

~~~clojure
{com.github.igrishaev/pg-client {:mvn/version "0.1.1"}
~~~

## Installing the Pool

The client depends on pool so there is no need to specify it explicitly.

~~~clojure
[com.github.igrishaev/pg-pool "0.1.1"]
~~~

Deps:

~~~clojure
{com.github.igrishaev/pg-pool {:mvn/version "0.1.1"}
~~~
