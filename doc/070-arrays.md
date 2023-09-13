# Arrays

<!-- toc -->

- [Usage](#usage)
- [Details](#details)

<!-- tocstop -->

In JDBC, arrays have always been a pain. Every time you want to pass an array to
the database or read it back, you've got to wrap your data with various Java
classes, extend protocols, and multimethods. We do it in each project, and it
doesn't have to be like this.

The recent release of PG ships a significant feature: arrays. You can pass an
array to a query and read it back as easily as they were native Clojure
vectors. No more ceremonies with classes, manual parsing, etc.

## Usage

Reading a trivial array of integers:

~~~clojure
(pg/execute conn "select '{1,2,3}'::int[] as array")

;; [{:array [1 2 3]}]
~~~

Reading an array of strings. Pay attention that the second item is NULL:

~~~clojure
(pg/execute conn "select '{foo,null,baz}'::text[] as array")

[{:array ["foo" nil "baz"]}]
~~~

Multi-dimensional arrays of any sub-type work as well. Below, we read a 2x2
array of dates:

~~~clojure
(pg/execute conn "select '{{2020-01-01,2021-12-31},{2099-11-03,1301-01-23}}'::date[][] as array")
[{:array
  [[#.j.t.LocalDate "2020-01-01"
    #.j.t.LocalDate "2021-12-31"]
   [#.j.t.LocalDate "2099-11-03"
    #.j.t.LocalDate "1301-01-23"]]}]
~~~

The same 2x2 array of UUIDs:

~~~clojure
(pg/execute conn "select '{{887dfa2b-ab88-47d6-ab2f-83b66685063e,9ae401db-95ee-4612-880c-011ad15cdacf},{2f15d54b-836d-426a-9389-b878f6b0aa18,88991362-20ff-4217-96d5-20bd70166916}}'::uuid[][] as array")

[{:array
  [[#uuid "887dfa2b-ab88-47d6-ab2f-83b66685063e"
    #uuid "9ae401db-95ee-4612-880c-011ad15cdacf"]
   [#uuid "2f15d54b-836d-426a-9389-b878f6b0aa18"
    #uuid "88991362-20ff-4217-96d5-20bd70166916"]]}]
~~~

To pass an array into a query, use a plain vector, a list, or a sequence:

~~~clojure
(pg/execute conn "select 2 = ANY ($1) as in_array" [[1 2 3]])
[{:in_array true}]
~~~

## Details

- In both reading and writing, arrays are represented with vectors. If more
  precisely, encoding logic relies on the `clojure.lang.Sequential`
  interface. It means lists, sets, and lazy sequences are supported as well. You
  can pass into a vector the result of `map`, `filter`, etc.

- Arrays might be multidimensional. Nested arrays are represented with
  vectors/lists as well.

- Arrays support both binary and text Postgres Wire protocols.

- Unlike JDBC, arrays might be of any type: timestamp(tz), UUID, numeric, text.
