# PG: a Postgres Driver in Pure Clojure and More

[postgres]: https://www.postgresql.org/

This project is a set of libraries related to the [PostgreSQL][postgres]
database. The primary library called `pg-client` is a driver for Postgres
written in pure Clojure. By purity I mean, neither JDBC nor any other
third-party Java libraries are involved. Everything is driven by a TCP socket
and implementation of the Posrgres Wire protocol. Fun!

Besides the client, the project provides such various additions as a connection
pool (see `pg-pool`). The `pg-types` library holds the encoding and decoding
logic which determines how to write and read Clojure data from and into the
database. You can use this library separately in pair with JDBC.next and `COPY`
for efficient data transcoding in binary format.

The question you would probably ask is, why would create a Postgres client from
scratch? JDBC has been around for decades, and there are also good
`clojure.java.jdbc` and `jdbc.next` wrappers on top of it?

The answer is: that although these two libraries are amazing, they don't
disclose all Postgres features. JDBC is an abstraction whose main goal is to
satisfy all the DB engines. A general library that works with MS SQL, MySQL, and
Postgres at the same time would reduce the variety of features each backend is
capable of.

Postgres is a great database that brings *an enormous amount* of features. I've
been working with Postgres a lot and in each project, I had to invent a wheel
from scratch: add JSON support, type mapping, smart `ResultSet` processing, and
so on. I strongly believe there should be a client that pairs Clojure with
Postgres seamlessly when all the features are available out of the box.

Here is a brief list of the benefits of this project:

- Written in pure Clojure and thus is completely transparent for users and
  developers;

- Supports a lot of Clojure and Java types including `java.time.*` (see the
  dedicated section in the documentation);

- Extendable encoding and decoding: adding a new type means just extending a
  multimethod;

- Implements both **Simple** and **Extended** Postgres Wire protocols;

- Implements both **text** and **binary** content encoding and decoding;

- Easy to debug what goes through the wire;

- Reducing the result on the fly, custom reducers; various ways to process the
  data;

- SAML authentication out from the box; JDBC still fails when handling it
  ("unknown auth code 10");

- Tested with Postgres 11, 12, 13, 14, 15, and 16-beta (integration tests with
  multiple Docker images);

- And more.
