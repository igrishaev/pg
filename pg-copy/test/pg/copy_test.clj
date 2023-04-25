(ns pg.copy-test
  (:import
   org.postgresql.copy.CopyManager)
  (:require
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
    (copy/table->file payload "out.bin" opt)

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
  (test-script "x double precision"
               [[(float 1.0)]
                [(float 2.0)]
                [(float 3.0)]]
               [{:x 1.0} {:x 2.0} {:x 3.0}]
               {:oids [oid/FLOAT8]}))

#_
(deftest test-double-float4
  (test-script "x float"
               [[1.1]
                [2.2]
                [3.3]]
               1))
