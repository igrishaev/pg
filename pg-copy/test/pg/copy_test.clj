(ns pg.copy-test
  (:import
   org.joda.time.LocalDate
   java.sql.Timestamp
   java.util.Date
   java.time.Instant
   org.postgresql.copy.CopyManager)
  (:require
   pg.joda-time
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]
   [pg.oid :as oid]
   [pg.copy :as copy]
   [clojure.test :refer [deftest is use-fixtures]]))


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
        (str (gensym "table"))

        sql-table
        (format "create temp table %s (%s)" table sql-fields)

        sql-copy
        (format "copy %s from stdin with binary" table)

        copy-mgr
        (new CopyManager conn)

        input
        (copy/table->input-stream payload opt)]

    ;; for debug
    ;; (copy/table->file payload "out.bin" opt)

    ;; (is (= 1 (jdbc/execute! conn ["select '1970-01-01'::timestamp"])))

    (jdbc/execute! conn [sql-table])
    (.copyIn copy-mgr sql-copy input)

    (let [result
          (jdbc/execute! conn
                         [(format "select * from %s" table)]
                         {:builder-fn rs/as-unqualified-maps})]

      (is (= expected result)))))


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
               {:oids [oid/INT4]}))


(deftest test-copy-long-as-int2
  (test-script "x smallint"
               [[1]
                [2]
                [3]]
               [{:x 1} {:x 2} {:x 3}]
               {:oids [oid/INT2]}))


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
               {:oids [oid/FLOAT8]}))


(deftest test-copy-double-float4
  (test-script "x real"
               [[1.0]
                [2.0]
                [3.0]]
               [{:x 1.0} {:x 2.0} {:x 3.0}]
               {:oids [oid/FLOAT4]}))


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
               {:oids [oid/UUID]}))


(deftest test-copy-uuid-to-string
  (test-script "x text"
               [[#uuid "415ca101-9d02-417d-91f8-36df31e9cb04"]]
               [{:x "415ca101-9d02-417d-91f8-36df31e9cb04"}]
               {:oids [oid/TEXT]}))


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
  (let [d (new Date (- 1969 1900) 1 1 0 0 0)]
    (test-script "x date"
                 [[d]]
                 [{:x d}]
                 {:oids [oid/DATE]})))


(deftest test-copy-instant-to-timestamp
  (let [secs -1
        nans 123456789
        inst (Instant/ofEpochSecond secs nans)]
    (test-script "x timestamp"
                 [[inst]]
                 [{:x #inst "1969-12-31T23:59:59.123456000-00:00"}])))


(deftest test-copy-yoda-ld-to-date

  (let [ld (new LocalDate 2021 3 15)]
    (test-script "x date"
                 [[ld]]
                 [1])

   )



  )
