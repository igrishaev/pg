# PG: Postgres-related libraries for Clojure

This repository holds a set of packages related to PostgreSQL. This a breakdown
of my (unsuccessful) attempt to write a PostgreSQL client in pure Clojure
(stored in the `_` directory). Although I didn't achieve the goal, some parts of
the code are now shipped as separated packages and might be useful for someone.

## Table of Contents

<!-- toc -->

- [pg-common](#pg-common)
  * [Installation](#installation)
  * [Usage](#usage)
- [pg-encode](#pg-encode)
  * [Installation](#installation-1)
  * [Usage](#usage-1)
  * [Type-specific encoding](#type-specific-encoding)
  * [Table of supported types and OIDs](#table-of-supported-types-and-oids)
- [pg-joda-time](#pg-joda-time)
- [pg-copy](#pg-copy)
- [pg-copy-jdbc](#pg-copy-jdbc)

<!-- tocstop -->

## pg-common

A set of common modules with utilities and constant. The most important
namespace is `pg.oid` which holds the registry of builtin OIDs in Postgres. It
has been generated out directly from the `pg_type.dat` file stored in the
official Postgres repository.

### Installation

Leiningen/Boot:

~~~clojure
[com.github.igrishaev/pg-common "0.1.0-SNAPSHOT"]
~~~

Clojure CLI/deps.edn:

~~~clojure
com.github.igrishaev/pg-common {:mvn/version "0.1.0-SNAPSHOT"}
~~~

### Usage

That's unlikely you'll need that package directly as the rest depend on it.

## pg-encode

A module to encode Clojure values into Postgres counterparts, both text and
binary. At the moment, supports only the binary format for primitives, UUIDs and
date/time types.

### Installation

~~~clojure
[com.github.igrishaev/pg-encode "0.1.0-SNAPSHOT"]
~~~

Clojure CLI/deps.edn:

~~~clojure
com.github.igrishaev/pg-encode {:mvn/version "0.1.0-SNAPSHOT"}
~~~

### Usage

Import the package:

~~~clojure
(require 'pg.encode.bin)
(in-ns 'pg.encode.bin)
~~~

The `encode` function, in its simple case, takes a value and returns a byte
array which represents that value in a Postgres-friendly binary format:

~~~clojure
(encode 1)
[0, 0, 0, 0, 0, 0, 0, 1]

(type (encode 1))
[B

(encode true)
[1]

(encode false)
[0]

(encode "hello")
[104, 101, 108, 108, 111]
~~~

Complex types like `Date` and `UUID` are supported as well:

~~~clojure
(encode (new java.util.Date))
[0, 2, -99, -97, -61, 76, -42, -104]

(encode (random-uuid))
[-89, 69, -70, 4, -61, -17, 71, 112, -94, -57, -6, 47, -42, 41, 24, 62]
~~~

### Type-specific encoding

### Table of supported types and OIDs

| Clojure | Postgres | Default? | Comment         |
|---------|----------|----------|-----------------|
|         |          |          |                 |
|         |          |          |                 |
| Long    | int8     | +        |                 |
| Long    | int4     |          | Cast to Integer |
| Long    | int2     |          | Cast to Short   |
| Integer | int8     |          | Cast to Long    |
| Integer | int4     | +        |                 |
| Integer | int2     |          | Cast to Short   |
| Short   | int8     |          | Cast to Long    |
| Short   | int4     |          | Cast to Integer |
| Short   | int2     | +        |                 |
|         |          |          |                 |
|         |          |          |                 |
|         |          |          |                 |
|         |          |          |                 |


## pg-joda-time

## pg-copy

## pg-copy-jdbc
