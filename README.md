
[pg2]: https://github.com/igrishaev/pg2

**Attention: this repository has been deprecated in favour of [PG2
project][pg2], which is a successor of PG(one). Should you have been using
PG(one) by any chance, please migrate to [PG2][pg2]!**

# PG: a Postgres Driver in Pure Clojure

The latest version is **0.1.11**.

## Package index

| Short Name     | Full Name & Version                            | Description                                                                           |
|----------------|------------------------------------------------|---------------------------------------------------------------------------------------|
| pg-common      | `[com.github.igrishaev/pg-common "0.1.11"]`    | Keeps the registry of all the known PostgreSQL OIDs; bytes utilities; codecs; macros. |
| pg-types       | `[com.github.igrishaev/pg-types "0.1.11"]`     | Implements text and binary encoding and decoding of Clojure and PG types.             |
| pg-client      | `[com.github.igrishaev/pg-client "0.1.11"]`    | The client library through which you connect to the database.                         |
| pg-pool        | `[com.github.igrishaev/pg-pool "0.1.11"]`      | The connection pool for the client.                                                   |
| pg-ssl         | `[com.github.igrishaev/pg-ssl "0.1.11"]`       | Custom SSL context for secure connection.                                             |
| pg-json        | `[com.github.igrishaev/pg-json "0.1.11"]`      | Extends `pg-types` with JSON encoding and decoding.                                   |
| pg-joda-time   | `[com.github.igrishaev/pg-joda-time "0.1.11"]` | Extends `pg-types` with Joda Time encoding and decoding.                              |
| pg-honey       | `[com.github.igrishaev/pg-honey "0.1.11"]`     | Integration with HoneySQL.                                                            |
| pg-integration | `[com.github.igrishaev/ "0.1.11"]`             | Utilities for integration tests (dev purpose only).                                   |

## Benefits

- Implements both simple and extended Postgres protocols;
- Supports both text and binary encoding and decoding;
- Extremely Clojure-friendly;
- Has its own connection pool;
- Allows to reduce the result as you want;
- Flexible and extendable;
- Supports (multi-dimensional) arrays;
- Convenient COPY IN/FROM functions;
- Rich types mapping;
- JSON support;
- Joda Time support;
- Easy integration with HoneySQL;
- and more (lots of good things in TODO).

## Documentation

- [About](doc/000-about.md)
- [Installation](doc/010-installation.md)
- [Client](doc/020-client.md)
- [Connection pool](doc/030-pool.md)
- [Notifications](doc/025-notifications.md)
- Types
- JSON
- Joda Time
- [Arrays](doc/070-arrays.md)
- [SSL](doc/080-ssl.md)
- [COPY functions](doc/090-copy.md)
- [HoneySQL](doc/100-honey.md)
