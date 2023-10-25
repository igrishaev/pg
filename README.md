# PG: a Postgres Driver in Pure Clojure

The latest version is **0.1.9**.

## Package index

| Short Name | Full Name & Version                        | Description                                                                           |
|------------|--------------------------------------------|---------------------------------------------------------------------------------------|
| pg-common  | `[com.github.igrishaev/pg-common "0.1.9"]` | Keeps the registry of all the known PostgreSQL OIDs; bytes utilities; codecs; macros. |
| pg-types   | `[com.github.igrishaev/pg-types "0.1.9"]`  | Implements text and binary encoding and decoding of Clojure and PG types.             |

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
