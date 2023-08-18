# PG: a Postgres Driver in Pure Clojure and More

[postgres]: https://www.postgresql.org/

This project is a set of libraries related to the [PostgreSQL][postgres]
database. The primary library called `pg-client` is a driver for Postgres
written in pure Clojure. By purity I mean, neither JDBC nor any other
third-party Java libraries are involved. Everything is hold by a TCP socket plus
implementation of Posrgres Wire protocol.

Besides the client, the project provides various additions like a connection
pool (see `pg-pool`). The `pg-types` library holds the encoding and decoding
logic which determines how to write and read Clojure data from and into the
database. You can use this library separately in pair with JDBC.next for
efficient data transcoding in binary format.

The question you would probably ask is, why would create a Postgres client from
scratch? JDBC has been for decades with us, and there are also good
`clojure.java.jdbc` and `jdbc.next` wrappers on top of it?

The answer is: although these two libraries are amazing, they don't disclose all
Postgres features. JDBC is an abstraction which main goal is to satisfy all the
DB engines. A general library that works with MS SQL, MySQL, and Postgres at the
same time would reduce the variety of features each backend might bring.

Postgres is a great database that carries *an anourmous amount* of fearues. I've
been working with Postgres a lot and in each project, I have to invent a wheel
from scratch: add JSON support, type mapping, smart `ResultSet` processing and
so on. I strongly believe there should be a client that pairs Postgres and
Clojure seamlessly, when all the features availabe out from the box.

Here is a brief list of benefits of this project:

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
- Holds SAML authentication which is default since Postgres 15. JDBC still fails
  when handling it ("unknown auth code 10");
- And more.

- PG pool
- Encoding and decoding values
- JSON support
- Joda Time support
- Copy (general)
- Copy with JDBC.Next
- Full package list
- Changelog
