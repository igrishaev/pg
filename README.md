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
  * [Extending the encoding rules](#extending-the-encoding-rules)
  * [Default OIDs](#default-oids)
- [pg-joda-time](#pg-joda-time)
  * [Installation](#installation-2)
  * [Usage](#usage-2)
  * [Table of Types and OIDs](#table-of-types-and-oids)
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

Sometimes you need precise on control on encoding. Say, a Long value 1 gets
encoded to int8 but you want it to be int4. The second argument of the `encode`
function takes an integer OID that specifies a column type. You can reach the
built-in OIDs using the `pg.oid` module.

Thus, to encode Long 1 as int4, do this:

~~~clojure
(encode 1 pg.oid/int4)
[0, 0, 0, 1]
~~~

To encode an integer as int8, do:

~~~clojure
(let [i (int 42)]
  (encode i pg.oid/int8))
[0, 0, 0, 0, 0, 0, 0, 42]
~~~

The same applies to Float and Double types. Float gets encoded to float4 by
default and Double does to float8. Passing explicit OIDs corrects the output
types.

~~~clojure
(encode 1.01 pg.oid/float4)
[63, -127, 71, -82]

(encode 1.01 pg.oid/float8)
[63, -16, 40, -11, -62, -113, 92, 41]
~~~

### Table of supported types and OIDs

At the moment of writing this, the module has the following mapping between
Clojure types and Postgres OIDs.

| Clojure       | Postgres            | Default   |
|---------------|---------------------|-----------|
| Symbol        | text, varchar       | text      |
| String        | text, varchar, uuid | text      |
| Character     | text, varchar       | text      |
| Long          | int8, int4, int2    | int8      |
| Integer       | int8, int4, int2    | int4      |
| Short         | int8, int4, int2    | int2      |
| Boolean       | bool                | bool      |
| Float         | float4, float8      | float4    |
| Double        | float4, float8      | float8    |
| UUID          | uuid, text          | uuid      |
| j.u.Date      | timestamp, date     | timestamp |
| j.t.Instant   | timestamp, date     | timestamp |
| j.t.LocalDate | date                | date      |

### Extending the encoding rules

Encoding a type that is missing the table above leads to an exception:

~~~clojure
(encode {:foo 42} pg.oid/json)

Execution error (ExceptionInfo) at pg.error/error! (error.clj:14).
Cannot binary encode a value

{:value {:foo 42}, :oid 114, :opt nil}
~~~

But it can easily fixed by extending the `-encode` multimethod from the
`pg.encode.bin` namespace. Its dispatch function takes a vector where the first
item is a type of the value and the second is OID:

~~~clojure
(defmethod -encode [UUID oid/uuid]
  [^UUID value oid opt]
  (let [most-bits (.getMostSignificantBits value)
        least-bits (.getLeastSignificantBits value)]
    (byte-array
     (-> []
         (into (array/arr64 most-bits))
         (into (array/arr64 least-bits))))))
~~~

To extend the encoder with a map such that it becomes JSON in Postgres, use
something like this:

~~~clojure
(defmethod -encode [clojure.lang.IPersistentMap pg.oid/json]
    [mapping _ _]
    (-> mapping
        (cheshire.core/generate-string )
        (.getBytes "UTF-8")))

(encode {:foo 42} pg.oid/json)
[123, 34, 102, 111, 111, 34, 58, 52, 50, 125]
~~~

Let's try the opposite: copy the output and restore the origin string:

~~~clojure
(-> [123, 34, 102, 111, 111, 34, 58, 52, 50, 125]
    (byte-array)
    (String. "UTF-8"))

"{\"foo\":42}"
~~~

### Default OIDs

When no OID is passed, it's nil. Thus, you must specify one more method for the
`[Type, nil]` pair. This method is used when the `encode` function is called
with only one argument.

To keep the code short, there is a shortcut `set-default` in `pg.encode.bin`
that takes a type, an OID and clones the method declared for this pair into the
`[Type nil]` pair. The corresponding `[Type OID]` method should be declared in
advance or you'll get an exception.

Here is an example for the Integer type. It has tree pairs for int8, int4 and
int2, and the int4 case is set as default.

~~~clojure
(defmethod -encode [Integer oid/int8]
  [value oid opt]
  (-encode (long value) oid opt))

(defmethod -encode [Integer oid/int4]
  [value oid opt]
  (array/arr32 value))

(defmethod -encode [Integer oid/int2]
  [value oid opt]
  (-encode (short value) oid opt))

(set-default Integer oid/int4)
~~~

## pg-joda-time

[joda-time]: https://www.joda.org/joda-time/

Extends the encoding protocols with [Joda Time][joda-time] types.

### Installation

Leiningen/Boot:

~~~clojure
[com.github.igrishaev/pg-joda-time "0.1.0-SNAPSHOT"]
~~~

Clojure CLI/deps.edn:

~~~clojure
com.github.igrishaev/pg-joda-time {:mvn/version "0.1.0-SNAPSHOT"}
~~~

### Usage

Import the `pg.joda-time` namespace to extend the `-encode` protocol mentioned
above.

~~~clojure
(encode (new org.joda.time.DateTime))
[0, 2, -99, -79, 88, 15, 48, -128]
~~~

### Table of Types and OIDs

| Clojure         | Postgres         | Default   |
|-----------------|------------------|-----------|
| o.j.t.LocalDate | date             | date      |
| o.j.t.LocalTime | time             | time      |
| o.j.t.DateTime  | timestamp        | timestamp |

## pg-copy

## pg-copy-jdbc
