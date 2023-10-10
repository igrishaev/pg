# COPY

<!-- toc -->

- [Theory](#theory)
- [CSV vs Binary](#csv-vs-binary)
- [Usage](#usage)
  * [COPY out](#copy-out)
  * [COPY IN from stream](#copy-in-from-stream)
  * [COPY IN rows](#copy-in-rows)
  * [COPY IN maps](#copy-in-maps)

<!-- tocstop -->

[CopyManager]: https://jdbc.postgresql.org/documentation/publicapi/org/postgresql/copy/CopyManager.html

## Theory

Since 0.1.9, the pg-client library supports various ways to COPY the data into
or from the database. It's much more flexible than the official JDBC Postgres
driver's standard [CopyManager][CopyManager] class.

To remind you, COPY is a massive way of writing or reading data. Copying IN is
much faster than inserting the rows by chunks. Postgres starts to read the data
immediately without waiting for the last bit of data to arrive. You can copy
into the same table in parallel threads. The same applies to copying out: if you
want to dump a table into a file, use COPY FROM with an OutputStream
OutputStream rather than selecting everything in memory.

The main disadvantage of JDBC CopyManager is, that it doesn't do anything about
data encoding and encoding. It accepts either an InputStream or an OutputStream
assuming you encode the data on your own. It means, right before you copy the
data to the database, you've got to manually encode them into CSV.

This is not as easy as you might think. When encoding values into CSV, it
coerces everything to a string using `str`. That's OK for most of the primitive
types as numbers, booleans or strings: their Clojure representation matches the
way they're represented in Postgres. But it doesn't work for complex types like
arrays. If you write a vector of `[1 2 3]` in CSV you'll get `"[1 2 3]"` which
is an improper Postgres value. It must have been `{1, 2, 3}` instead.

[copy-docs] https://postgrespro.ru/docs/postgrespro/14/protocol-flow?lang=en#PROTOCOL-COPY

Another flaw of JDBC CopyManager is, that it doesn't split the data by rows when
sending them into the database. It simply reads 2Kb of bytes from an InputStream
and writes them to a socket. At the same time, the PostgreSQL documentation
[recommends][copy-docs] splitting the data chunks by rows:

> The message boundaries are not required to have anything to do with row
> boundaries, although that is often a reasonable choice

Moreover, PostgreSQL supports not only CSV but also text and binary formats. The
text format is somewhat CSV with different separators so it's not so
important. But the binary format *is* indeed! Binary-encoded data are faster to
parse and process and thus are preferable when dealing with vast chunks of data.

## CSV vs Binary

Here are a couple of measurements I made on my local machine. I made two files
containing 10 million rows: in CSV and in binary format. Then I used the
official CopyManager to copy these files in the database. All the server
settings were default; the machine was an Apple M1 Max 32Gb with 10 Cores.

**Single thread COPY**

| Rows  | Format | Time, sec |
|-------|--------|-----------|
| 10M   | binary | 17.4      |
| 10M   | CSV    | 51.2      |

**Parallel COPY**

Binary:

| Rows  | Threads | Chunk | Format | Time, sec |
|-------|---------|-------|--------|-----------|
| 10M   | 8       | 10k   | binary | 11.3      |
| 10M   | 4       | 10k   | binary | 13.7      |
| 10M   | 1       | 10k   | binary | 28.6      |

CSV:

| Rows  | Threads | Chunk | Format | Time, sec |
|-------|---------|-------|--------|-----------|
| 10M   | 8       | 10k   | CSV    | 10.6      |
| 10M   | 4       | 10k   | CSV    | 19.9      |
| 10M   | 1       | 10k   | CSV    | 71.7      |

It's plain to see that binary encoding is three times faster than CSV. 17 vs 51 seconds is a significant difference one cannot ignore.

The good news is, the PG library does support binary encoding. It also allows you to perform COPY operations without encoding them manually. The library doesn't make any InputStreams in the background: it encodes the rows one by one and sends them directly into the database. It also supports binary format of encoding which is a matter of passing a parameter. Also, it does split the data chunks by rows, not by the size of the buffer.


## Usage

Establish a connection to the database first:

~~~clojure
(require '[pg.client :as pg])

(def conn (pg/connect {...}))
~~~

### COPY out

The `copy-out` function dumps a table or a query into a file. It accepts a
connection object, a SQL expression describing the table, the columns, the
format and other details, and an instance of an OutputStream. The rows from the
table or a query get sent to that stream. The function returns a number of rows
processed.

~~~clojure
(let [sql
      "COPY (select s.x as x, s.x * s.x as square from generate_series(1, 9) as s(x))
      TO STDOUT WITH (FORMAT CSV)"

      out
      (new ByteArrayOutputStream)]

  (pg/copy-out conn sql out))
~~~

The expression above returns 9 (the number of rows). The actual rows are now in
the `out` variable that stores bytes.

Of course, for massive data it's better to use not `ByteArrayOutputStream` but
`FileOutputStream`. You can produce it as follows:

~~~clojure
(with-open [out (-> "/some/file.csv"
                    io/file
                    io/output-stream)]
  (pg/copy-out conn sql out))
~~~

The PG library doesn't close the stream assuming you may write multiple data
into a single stream. It's up to you when to close it.

To dump the data into a binary file, add the `WITH (FORMAT BINARY)` clause to
the SQL expression. Binary files are more difficult to parse yet they're faster
in processing.

### COPY IN from stream

The `copy-in` function copies the data from in InputStream into the
database. The payload of the stream is either produced by the previous
`copy-out` function or manually by dumping the data into CSV/binary format. The
function returns the number or rows processed by the server.

~~~clojure
(def in-stream
  (-> "/some/file.csv" io/file io/input-stream))

(pg/copy-in conn
            "copy foo (id, name, active) from STDIN WITH (FORMAT CSV)"
            in-stream)

;; returns 6
~~~

Again, it doesn't close the input stream. Use the `with-open` macro to close it
explicitly.

The next two functions are more interesting as they bring functionality missing
in the JDBC.

### COPY IN rows

The `copy-in-rows` function takes a sequence of rows and sends them into the
database one by one. It doesn't do any intermediate steps like dumping them into
an InputStream first. Everything is done on the fly.

The function takes a connection, a SQL expression, and a sequence of rows. A row
is a sequence of values. The result is a number of rows copied into the
database.

~~~clojure
(pg/copy-in-rows conn
                 "copy foo (id, name, active, note) from STDIN WITH (FORMAT CSV)"
                 [[1 "Ivan" true nil]
                  [2 "Juan" false "kek"]])
;; 2
~~~

The fourth optional parameter is a map of options. At the moment, the following
options are supported:

| name      | default      | example (or enum)                                     | description                                                                                |
|-----------|--------------|-------------------------------------------------------|--------------------------------------------------------------------------------------------|
| `:sep`    | ,            |                                                       | a character to separate columns in CSV/text formats                                        |
| `:end`    | `\r\n`       |                                                       | a line-ending sequence of characters in CSV/text                                           |
| `:null`   | empty string |                                                       | a string to represent NULL in CSV/text                                                     |
| `:oids`   | `nil`        | `[oid/int2 nil oid/date]`, `{0 oid/int2, 2 oid/date}` | type hints for proper value encoding. Either a vector or OIDs, or a map of {index => OID}  |
| `:format` | `:csv`       | `:csv`, `:bin`, `:txt`                                | a keyword to specify the format of a payload.                                           |

Copy rows in CSV with custom column separators and NULL representation:

~~~clojure
(pg/copy-in-rows conn
                 "COPY foo (id, name, active, note) FROM STDIN WITH (FORMAT CSV, NULL 'NULL', DELIMITER '|')"
                 rows
                 {:null "NULL"
                  :sep \|})
;; 1000
~~~

Copy rows as a binary payload with custom type hints:

~~~clojure
(pg/copy-in-rows conn
                 "COPY foo (id, name, active, note) from STDIN WITH (FORMAT BINARY)"
                 rows
                 {:format :bin
                  :oids {0 oid/int2 2 oid/bool}})
;; 1000
~~~

### COPY IN maps

Often, we deal not with plain rows but maps. The `copy-in-maps` function acts
but `copy-in-rows` but accepts a sequence of maps. Internally, all the maps get
transformed into rows. To transform it properly, the function needs to know the
order of the keys.

The funtion accepts a connection, a SQL expression, a sequence of maps and a
sequence of keys. Internally, it produces a selector from the keys like this:
`(apply juxt keys)` which gets applied to each map.

One more thing about copying maps is, that the `:oids` parameter is a map like
{key => OID}.

An example of copying the maps in CSV. Pay attention that the second map has
extra keys which are ignored.

~~~clojure
(pg/copy-in-maps conn
                 "copy foo (id, name, active, note) from STDIN WITH (FORMAT CSV)"
                 [{:id 1 :name "Ivan" :active true :note "aaa"}
                  {:aaa false :id 2 :active nil :note nil :name "Juan" :extra "Kek" :lol 123}]
                 [:id :name :active :note]
                 {:oids {:id oid/int2}
                  :format :csv})
~~~

Another example where we copy maps using binary format. The `:oids` map has a
single type hint so the `:id` fields get transformed to int2 but not bigint
which is default for Long values.

~~~clojure
(pg/copy-in-maps conn
                 "copy foo (id, name, active, note) from STDIN WITH (FORMAT BINARY)"
                 maps
                 [:id :name :active :note]
                 {:oids {:id oid/int2}
                  :format :bin})
~~~
