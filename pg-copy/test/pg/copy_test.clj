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


(deftest test-copy-biging
  (test-script "id bigint"
               [[1]
                [2]
                [3]]
               [{:id 1} {:id 2} {:id 3}]))


(deftest test-copy-integer-explicit
  (test-script "id integer"
               [[(int 1)]
                [(int 2)]
                [(int 3)]]
               [{:id 1} {:id 2} {:id 3}]))


(deftest test-copy-integer-oids
  (test-script "id integer"
               [[1]
                [2]
                [3]]
               [{:id 1} {:id 2} {:id 3}]
               {:oids {0 oid/INT4}}))
