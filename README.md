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
  * [Installation](#installation-3)
  * [Usage](#usage-3)
  * [OID hints](#oid-hints)
  * [Hints in metadata](#hints-in-metadata)
  * [Working with maps](#working-with-maps)
  * [Other functions](#other-functions)
- [pg-copy-jdbc](#pg-copy-jdbc)
  * [Installation](#installation-4)
  * [Usage](#usage-4)
  * [Parallel COPY](#parallel-copy)
  * [Measurements](#measurements)

<!-- tocstop -->

## pg-common

A set of common modules with utilities and constants. The most important
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

A module to prepare an InputStream to be `COPY`ied into the database. Uses the
binary format and thus is a bit faster than CSV.

### Installation

Leiningen/Boot:

~~~clojure
[com.github.igrishaev/pg-copy "0.1.0-SNAPSHOT"]
~~~

Clojure CLI/deps.edn:

~~~clojure
com.github.igrishaev/pg-copy {:mvn/version "0.1.0-SNAPSHOT"}
~~~

### Usage

The `pg.copy` namespace provides a set of functions related to the `COPY`
Postgres operator.

The main `data->input-stream` function takes data and returns an input stream
that is passed to `CopyManager`. The data must be a sequence of sequences each
of the same size. The stream contains a binary payload that Postgres knows how
to parse. Assuming you have a table with `bigint`, `text` and `bool` fields,
here is how building an input stream looks like:

~~~clojure
(def data
  [[1 "User 1" true]
   [2 "User 2" false]
   [3 "User 3" nil]])

(def input
  (data->input-stream data))

(def sql-copy
  "COPY users(id, name, active) FROM STDIN WITH BINARY")

(def copy-mgr
  (new CopyManager <tcp-conn>))

(.copyIn copy-mgr sql-copy input)
~~~

If the data matches the table, you can skip the columns in the COPY expression
ans type just `COPY users FROM`. But they are mandatory when you insert a
partial subset of columns in another order:

~~~clojure
(def data
  [["User 1" 1]
   ["User 2" 2]])

...

(def sql-copy
  "COPY users(name, id) FROM STDIN WITH BINARY")
~~~

### OID hints

Imagine you have a column of `integer` in a table. This type consists from 4
bytes whereas the standard Long type in Java consists from 8 bytes. If you
encode a Long value and COPY it into Postgres, it will argue on the payload
saying it's incorrect.

To solve the problem, either you coerce a Long value to Integer or, which is
better and simpler, specify that the Long column must be encoded as
Integer. This is know as OID hints.

The `data->input-stream` function accepts a map of options. The `:oids` field
might be either a vector or a map of Postgres OIDs:

~~~clojure
(def oids [oid/int4])

(def data
  [[1 "User 1" true]
   [2 "User 2" false]
   [3 "User 3" nil]])

(def input
  (data->input-stream data {:oids oids}))
~~~

Above, we specify that the first column (Long values 1, 2, 3) must be encoded as
4-byte integers.

It's not necessary to specify OIDs for all columns. Internally, the vector is
passed into the `(get oids i)` form where the `i` is an index of a column. A nil
OID stands for the default encoding rule.

Another example where you specify the type the third column:

~~~clojure
(def oids [nil nil oid/int4])

(def data
  [["User 1" true  1]
   ["User 2" false 2]
   ["User 3" nil   3]])
~~~

You can also use a map of index &rarr; OID where the index starts from zero:

~~~clojure
(def oids {2 oid/int4})

(def data
  [["User 1" true  1]
   ["User 2" false 2]
   ["User 3" nil   3]])
~~~

Finally, OID hints might carry not integer OIDs but their names as well, for
example:

~~~clojure
[nil nil "int4"]

{2 "int4"}
~~~

See the `pg.oid` for their names and values.

### Hints in metadata

When the `:oids` field is not passed, the library makes an attempt to fetch the
hints from the metadata of a matrix. Their field is `:pg/oids`. There is a
function `with-oids` that supplies a matrix with the type hints as follows:

~~~clojure
(def data
  (with-oids
    [["User 1" true  1]
     ["User 2" false 2]
     ["User 3" nil   3]]
    {2 oid/int4}))
~~~

Then you passed the data into the `data->input-stream` function without the
`:oids` option.

### Working with maps

The code shown above works with matrices although most often we deal with
maps. The former might be transformed to the latter with a helper function
called `maps->data`. It takes a seq of maps, the keys to select, and,
optionally, a map of key => OID for encoding.

~~~clojure
(def rows
  [{:name "User 1" :id 1 :active true}
   {:name "User 2" :id 2 :active false}
   {:name "User 3" :id 3 :active nil}])

(maps->data rows [:id :name])

([1 "User 1"]
 [2 "User 2"]
 [3 "User 3"])
~~~

An example with the third argument produces the same result but with an
additional field in its metadata:

~~~clojure
(maps->data rows [:id :name] {:id "int4"})

(meta *1)

{:pg/oids ["int4" nil]}
~~~

Since the matrix is already charged with OID hints, there is no need to pass
them into the `:oids` option.

### Other functions

`data->bytes` acts the same but dumps the payload into a byte array:

~~~clojure
(data->bytes [[1 "User 1" true]])

[80, 71, 67, 79, 80, 89, 10, -1, 13, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0,
 0, 8, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 6, 85, 115, 101, 114, 32, 49, 0, 0, 0,
 1, 1, -1, -1]
~~~

`data->file` saves the binary payload into a file:

~~~clojure
(data->file [[1 "User 1" true]] "out.bin")
~~~

~~~bash
> od -c out.bin

0000000    P   G   C   O   P   Y  \n 377  \r  \n  \0  \0  \0  \0  \0  \0
0000020   \0  \0  \0  \0 001  \0  \0  \0 001 001  \0 001  \0  \0  \0 001
0000040   \0  \0 001 377 377 377 377 377 377
~~~

A more general function `data->input-stream` redirects the payload into an
instance of an `OutputStream`.

All the functions accept an additional map of options with the `:oids` field.

## pg-copy-jdbc

### Installation

### Usage

### Parallel COPY

### Measurements
