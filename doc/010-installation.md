# Installation

<!-- toc -->

- [General Notes](#general-notes)
- [Client](#client)
- [Connection Pool](#connection-pool)
- [JSON extension](#json-extension)
- [Joda Time extension](#joda-time-extension)
- [SSL Context extension](#ssl-context-extension)

<!-- tocstop -->

## General Notes

Although PG is a set of modules, they all have the same version when deployed to
Clojars. For example:

Lein:

~~~clojure
[com.github.igrishaev/pg-client "0.1.9"]
[com.github.igrishaev/pg-pool "0.1.9"]
~~~

Deps:

~~~clojure
{com.github.igrishaev/pg-client {:mvn/version "0.1.9"}}
~~~

Lein users may specify a global `pg-version` variable on top of the
`project.clj` and reference it as follows:

~~~clojure
;; project.clj
(def pg-version "0.1.9")

(defproject ...
  :dependencies
  [...
   [com.github.igrishaev/pg-client ~pg-version]
   [com.github.igrishaev/pg-pool ~pg-version]])
~~~

Usually, you don't need to specify more than one package because they depend on
each other and will be download automatically.

## Client

The `pg-client` module ships a client access to Postgres. Since the connection
pool depends on logging facility, it's shipped in a separate package.

~~~clojure
[com.github.igrishaev/pg-client "0.1.9"]
~~~

Deps:

~~~clojure
{com.github.igrishaev/pg-client {:mvn/version "0.1.9"}}
~~~

## Connection Pool

The client depends on pool so there is no need to specify it explicitly.

~~~clojure
[com.github.igrishaev/pg-pool "0.1.9"]
~~~

Deps:

~~~clojure
{com.github.igrishaev/pg-pool {:mvn/version "0.1.9"}}
~~~

## JSON extension

A package that extends the client with reading and writing JSON objects.

~~~clojure
[com.github.igrishaev/pg-json "0.1.9"]
~~~

Deps:

~~~clojure
{com.github.igrishaev/pg-json {:mvn/version "0.1.9"}}
~~~

## Joda Time extension

Extends the client with Joda Time support.

~~~clojure
[com.github.igrishaev/pg-joda-time "0.1.9"]
~~~

Deps:

~~~clojure
{com.github.igrishaev/pg-joda-time {:mvn/version "0.1.9"}}
~~~

## SSL Context extension

A helper to build a custom instance of `SSLContext` out from your local key and
certificates.

~~~clojure
[com.github.igrishaev/pg-ssl "0.1.9"]
~~~

Deps:

~~~clojure
{com.github.igrishaev/pg-ssl {:mvn/version "0.1.9"}}
~~~
