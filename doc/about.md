# PG: a Postgres Driver in Pure Clojure and More

[postgres]: https://www.postgresql.org/

This project is a set of libraries related to the [PostgreSQL][postgres]
database. The primary library called `pg-client` is a driver for Postgres
written in pure Clojure. By purity I mean, neither JDBC nor any other
third-party Java libraries are involved. Everything is hold by a TCP socket plus
Posrgres protocol implementation.

Besides the client, the project provides such various additions as a connection
pool (see `pg-pool`). The `pg-types` library holds the encoding and decoding
logic which determines how to write and read Clojure data from or into the
database. You can use this library separately in pair with JDBC.next for
efficient data transcoding in binary format.

The main question you would probably ask is, why would create a Postgres client
from scratch? JDBC has been with us for decades, and there are good
`clojure.java.jdbc` and `jdbc.next`?

The answer is: although these two libraries are amazing, they don't disclose all
the Postgres features. JDBC is an abstraction which main goal is to satisfy all
the database. Writing a general library that works with MS SQL, MySQL, and
Postgres would reduce the variety of features each backend brings.

Postgres is great database that instroduces an anourmous amount of fearues. I've
bee working with Postgres and in each project, I had to reinvent a wheel: add
JSON support, various type mapping, smart `ResultSet` processing and so on. I
strongly believe there should be a client that connects Postgres and Clojure
seamlessly, when all the PG features availabe out from the box.

Here is a brief list of benefits of the project:

- Written in pure Clojure and thus is completely transparent for users and
  developers;
- Supports quite many Clojure and Java types including `java.time.*` (see the
  dedicated section in the documentation);
- Extendable encoding and decoding: adding a new type means just extending a
  multimethod;
- Implements both Simple and Extended Postgres protocols;
- Implements both text and binary encoding and decoding;
- Easy to debug what goes thought the wire;
- Reducing the result on the fly, custom reducers;
- Various ways to process the data;
- and more.

TO BE WRITTEN:

- Installation
- PG client
- PG pool
- Encoding and decoding values
- JSON support
- Joda Time support
- Copy (general)
- Copy with JDBC.Next
- Full package list
- Changelog
