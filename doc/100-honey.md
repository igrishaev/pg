# HoneySQL support

<!-- toc -->

- [About](#about)
- [Installation](#installation)
- [Usage](#usage)

<!-- tocstop -->

## About

HoneySQL is a well-known library to build SQL expression out from Clojure
maps. It's quite convenient for building complext queries, for example, when you
have optional JOIN operators. Or you don't know the final WHERE conditions as
the filtering parameters come from the request. HoneySQL makes you free from
from building raw SQL queries by concatenating strings which is unsafe and leads
to SQL injections.

The `pg-honey` is a small wrapper on top of HoneySQL. It provides its own
version of `query` and `execute` functions that accept not a SQL string but
Clojure maps. The maps get transformed into SQL under the hood and get executed.

## Installation

Install the `pg-honey` package as follows.

Lein:

~~~clojure
[com.github.igrishaev/pg-honey "0.1.9"]
~~~

Deps:

~~~clojure
{com.github.igrishaev/pg-honey {:mvn/version "0.1.9"}}
~~~

## Usage

TODO
