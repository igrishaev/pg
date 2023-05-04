(ns pg.copy-jdbc-test
  (:import
   java.sql.Timestamp
   java.time.Instant
   java.util.Date
   org.joda.time.DateTime
   org.joda.time.LocalDate
   org.postgresql.jdbc.PgConnection
   org.postgresql.copy.CopyManager)
  (:require
   [clojure.instant :as inst]
   [clojure.test :refer [deftest is use-fixtures]]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]
   [pg.copy :as copy]
   [pg.copy.jdbc :as copy.jdbc]
   [pg.joda-time]
   [pg.oid :as oid]))


(def db-spec
  {:dbtype "postgres"
   :port 35432
   :dbname "test"
   :user "test"
   :password "test"})


(defn test-script [sql-fields
                   payload
                   expected
                   & [opt]]
  (let [conn
        (jdbc/get-connection db-spec)

        table
        (str "table" (System/nanoTime))

        sql-table
        (format "create temp table %s (%s)" table sql-fields)

        sql-copy
        (format "copy %s from stdin with binary" table)

        copy-mgr
        (new CopyManager conn)

        input
        (copy/data->input-stream payload opt)]

    ;; for debug
    ;; (copy/data->file payload "out.bin" opt)

    (jdbc/execute! conn [sql-table])
    (.copyIn copy-mgr sql-copy input)

    (let [result
          (jdbc/execute! conn
                         [(format "select * from %s" table)]
                         {:builder-fn rs/as-unqualified-maps})]

      (is (= expected result)))

    (jdbc/execute! conn [(format "drop table %s" table)])))


(deftest test-copy-string
  (test-script "a text, b varchar, c char(8), d text"
               [["aaa" "bbb" "ccc" nil]]
               [{:a "aaa", :b "bbb", :c "ccc     ", :d nil}]))


(deftest test-copy-character
  (test-script "a text, b varchar, c char(8), d text"
               [[\A \B \C \D]]
               [{:a "A", :b "B", :c "C       ", :d "D"}]))


(deftest test-copy-symbol
  (test-script "a text, b varchar, c char(8)"
               [['foo 'test/foo 'kek/lol]]
               [{:a "foo", :b "test/foo", :c "kek/lol "}]))


(deftest test-copy-long
  (test-script "x bigint"
               [[1]
                [2]
                [3]]
               [{:x 1} {:x 2} {:x 3}]))


(deftest test-copy-long-as-int4
  (test-script "x integer"
               [[1]
                [2]
                [3]]
               [{:x 1} {:x 2} {:x 3}]
               {:oids [oid/int4]}))


(deftest test-copy-long-as-int2
  (test-script "x smallint"
               [[1]
                [2]
                [3]]
               [{:x 1} {:x 2} {:x 3}]
               {:oids [oid/int2]}))


(deftest test-copy-float
  (test-script "x real"
               [[(float 1.0)]
                [(float 2.0)]
                [(float 3.0)]]
               [{:x 1.0} {:x 2.0} {:x 3.0}]))


(deftest test-copy-float-to-integer-weird
  (test-script "x integer"
               [[(float 1.0)]
                [(float 2.0)]
                [(float 3.0)]]
               [{:x 1065353216}
                {:x 1073741824}
                {:x 1077936128}]))


(deftest test-copy-float-to-double
  (test-script "x float"
               [[(float 1.0)]
                [(float 2.0)]
                [(float 3.0)]]
               [{:x 1.0} {:x 2.0} {:x 3.0}]
               {:oids [oid/float8]}))


(deftest test-copy-double-float4
  (test-script "x real"
               [[1.0]
                [2.0]
                [3.0]]
               [{:x 1.0} {:x 2.0} {:x 3.0}]
               {:oids [oid/float4]}))


(deftest test-copy-double-float8
  (test-script "x float"
               [[1.1]
                [2.2]
                [3.3]]
               [{:x 1.1} {:x 2.2} {:x 3.3}]))


(deftest test-copy-bool
  (test-script "x boolean"
               [[true]
                [false]
                [nil]]
               [{:x true} {:x false} {:x nil}]))


(deftest test-copy-uuid
  (test-script "x UUID"
               [[#uuid "415ca101-9d02-417d-91f8-36df31e9cb04"]
                [nil]]
               [{:x #uuid "415ca101-9d02-417d-91f8-36df31e9cb04"}
                {:x nil}]))


(deftest test-copy-uuid-from-string
  (test-script "x UUID"
               [["415ca101-9d02-417d-91f8-36df31e9cb04"]]
               [{:x #uuid "415ca101-9d02-417d-91f8-36df31e9cb04"}]
               {:oids [oid/uuid]}))


(deftest test-copy-uuid-to-string
  (test-script "x text"
               [[#uuid "415ca101-9d02-417d-91f8-36df31e9cb04"]]
               [{:x "415ca101-9d02-417d-91f8-36df31e9cb04"}]
               {:oids [oid/text]}))


(deftest test-copy-date-to-timestamp-after-epoch
  (let [d (new Date (- 2000 1900) 0 1 23 59 59)]
    (test-script "x timestamp without time zone"
                 [[d]]
                 [{:x d}])))


(deftest test-copy-date-to-timestamp-before-epoch
  (let [d (new Date (- 1935 1900) 0 1 23 59 59)]
    (test-script "x timestamp without time zone"
                 [[d]]
                 [{:x d}])))


(deftest test-copy-date-to-date
  (let [d (new Date (- 1969 1900) 0 1 0 0 0)]
    (test-script "x date"
                 [[d]]
                 [{:x d}]
                 {:oids [oid/date]})))


(deftest test-copy-instant-to-timestamp
  (let [secs -1
        nans 123456000
        inst (Instant/ofEpochSecond secs nans)]
    (test-script "x timestamp"
                 [[inst]]
                 [{:x (inst/read-instant-timestamp (str inst))}])))


(deftest test-copy-yoda-ld-to-date
  (let [ld (new LocalDate 1969 1 1)
        inst (.toDate ld)]
    (test-script "x date"
                 [[ld]]
                 [{:x inst}])))


(deftest test-copy-yoda-ts-to-timestamp
  (let [dt (new DateTime 1969 3 15 23 59 59 123)
        inst (inst/read-instant-timestamp (str dt))]
    (test-script "x timestamp"
                 [[dt]]
                 [{:x inst}])))


(deftest test-copy-jdbc

  (with-open [conn (jdbc/get-connection db-spec)]

    (let [data
          [[1 "hello" true]
           [2 "haha!" false]]

          table
          (str "table" (System/nanoTime))

          sql-table
          (format "create temp table %s (a integer, x timestamp, b text, y uuid, c bool)" table)

          sql-copy
          (format "copy %s (a, b, c) from stdin with binary" table)

          _
          (jdbc/execute! conn [sql-table])

          result
          (copy.jdbc/copy-in conn sql-copy (copy/with-oids data {0 oid/int4}))]

      (is (= 2 result)))))


(deftest test-copy-jdbc-parallel

  (let [ds
        (jdbc/get-datasource db-spec)

        total
        99999

        data
        (for [x (range 0 total)]
          [x (str "hello_" x) (> (rand) 0.5)])

        table
        (str "table" (System/nanoTime))

        sql-table
        (format "create table %s (a integer, x timestamp, b text, y uuid, c bool)" table)

        sql-copy
        (format "copy %s (a, b, c) from stdin with binary" table)

        _
        (jdbc/execute! ds [sql-table])

        result
        (copy.jdbc/copy-in-parallel
         ds
         sql-copy
         data
         4
         10000
         {:oids {0 oid/int4 1 oid/text 2 oid/bool}})]

    (jdbc/execute! ds [(format "drop table %s" table)])

    (is (= result total))))


(deftest test-copy-jdbc-parallel-maps

  (let [ds
        (jdbc/get-datasource db-spec)

        total
        99

        maps
        (for [x (range 0 total)]
          {:a x :b (str "hello_" x) :c (> (rand) 0.5)})

        data
        (copy/maps->data maps [:a :b :c] {:a oid/int4 :b oid/text :c oid/bool})

        table
        (str "table" (System/nanoTime))

        sql-table
        (format "create table %s (a integer, x timestamp, b text, y uuid, c bool)" table)

        sql-copy
        (format "copy %s (a, b, c) from stdin with binary" table)

        _
        (jdbc/execute! ds [sql-table])

        result
        (copy.jdbc/copy-in-parallel
         ds
         sql-copy
         data
         4
         5)]

    (jdbc/execute! ds [(format "drop table %s" table)])

    (is (= result total))))


(def coerce-oids
  @#'copy/coerce-oids)


(deftest test-coerce-oids

  (is (= {0 25 1 16}
         (coerce-oids [oid/text oid/bool])))

  (is (= {0 16 1 23}
         (coerce-oids [oid/bool "int4"])))

  (is (= {5 1114}
         (coerce-oids {5 oid/timestamp}))))



(deftest test-table-oids

  (let [conn
        (jdbc/get-connection db-spec)

        table
        (str "table" (System/nanoTime))

        sql-table
        (format "create table %s (a integer, x timestamp, b text, y uuid, c bool)" table)

        _
        (jdbc/execute! conn [sql-table])

        result
        (copy.jdbc/table-oids conn table)]

    (jdbc/execute! conn [(format "drop table %s" table)])

    (is (= [[:a "int4"]
            [:x "timestamp"]
            [:b "text"]
            [:y "uuid"]
            [:c "bool"]]
           result))))
