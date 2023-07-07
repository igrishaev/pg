(ns pg.client.client-test
  (:require
   ;; pg.json
   [pg.client.api :as api]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]))


(def CONFIG
  {:host "127.0.0.1"
   :port 15432
   :user "ivan"
   :password "ivan"
   :database "ivan"})


#_
(def CONFIG
  {:host "127.0.0.1"
   :port 35432
   :user "test"
   :password "test"
   :database "test"})


(defn gen-table []
  (format "table_%s" (System/nanoTime)))


(deftest test-client-tx-status

  (api/with-connection [conn CONFIG]

    (is (= :I (api/status conn)))

    (api/query conn "select 1")

    (is (= :I (api/status conn)))

    (api/begin conn)

    (is (= :T (api/status conn)))

    (try
      (api/query conn "selekt 1")
      (catch Exception _
        nil))

    (is (= :E (api/status conn)))

    (api/rollback conn)

    (is (= :I (api/status conn)))))


(deftest test-client-ok

  (let [result
        (api/with-connection [conn CONFIG]
          (api/query conn "select 1 as foo, 'hello' as bar"))]

    (is (= [{:foo 1 :bar "hello"}]
           result))))


(deftest test-client-empty-query

  (let [result
        (api/with-connection [conn CONFIG]
          (api/query conn ""))]

    (is (nil? result))))


(deftest test-client-fn-column

  (let [result
        (api/with-connection [conn CONFIG]
          (api/query conn "select 1 as foo" {:fn-column str/upper-case}))]

    (is (= [{"FOO" 1}] result))))


(deftest test-client-exception-in-the-middle

  (api/with-connection [conn CONFIG]

    (is (thrown?
         Exception
         (with-redefs [pg.decode.txt/-decode
                       (fn [& _]
                         (throw (new Exception "boom")))]
           (api/query conn "select 1 as foo"))))

    (let [result
          (api/query conn "select 2 as bar")]

      (is (= [{:bar 2}] result)))))


(deftest test-client-reuse-conn

  (api/with-connection [conn CONFIG]

    (let [res1
          (api/query conn "select 1 as foo")

          res2
          (api/query conn "select 'hello' as bar")]

      (is (= [{:foo 1}] res1))
      (is (= [{:bar "hello"}] res2)))))


(deftest test-client-with-tx-syntax-issue
  (api/with-connection [conn CONFIG]
    (api/with-tx [conn]
      (is (map? conn)))))


(deftest test-client-with-transaction-ok

  (api/with-connection [conn CONFIG]

    (let [res1
          (api/with-tx [conn]
            (api/query conn "select 1 as foo" {:fn-result first}))

          res2
          (api/with-tx [conn]
            (api/query conn "select 2 as bar" {:fn-result first}))]

      (is (= {:foo 1} res1))
      (is (= {:bar 2} res2)))))


(deftest test-client-with-transaction-read-only

  (api/with-connection [conn CONFIG]

    (let [res1
          (api/with-tx [conn {:read-only? true}]
            (api/query conn "select 1 as foo"))]

      (is (= [{:foo 1}] res1))

      (try
        (api/with-tx [conn {:read-only? true}]
          (api/query conn "create temp table foo123 (id integer)"))
        (is false "Must have been an error")
        (catch Exception e
          (is (= "ErrorResponse" (ex-message e)))
          (is (= {:error
                  {:msg :ErrorResponse,
                   :errors
                   {:severity "ERROR"
                    :verbosity "ERROR"
                    :code "25006"
                    :message "cannot execute CREATE TABLE in a read-only transaction",
                    :function "PreventCommandIfReadOnly"}}}
                 (-> e
                     (ex-data)
                     (update-in [:error :errors] dissoc :line :file)))))))))


(deftest test-client-with-transaction-iso-level

  (let [table
        (gen-table)]

    (api/with-connection [conn CONFIG]

      (api/query conn (format "create table %s (id integer)" table))

      (api/with-tx [conn {:isolation-level "serializable"}]
        (api/query conn (format "insert into %s values (1), (2)" table))

        (let [res1
              (api/query conn (format "select * from %s" table))

              res2
              (api/with-connection [conn2 CONFIG]
                (api/query conn2 (format "select * from %s" table)))]

          (is (= [{:id 1} {:id 2}] res1))
          (is (= [] res2)))))))


(deftest test-client-with-transaction-rollback

  (let [table
        (gen-table)]

    (api/with-connection [conn CONFIG]

      (api/query conn (format "create table %s (id integer)" table))

      (api/with-tx [conn {:rollback? true}]
        (api/query conn (format "insert into %s values (1), (2)" table)))

      (let [res1
            (api/query conn (format "select * from %s" table))]

        (is (= [] res1))))))


(deftest test-client-create-table
  (api/with-connection [conn CONFIG]

    (let [table
          (gen-table)

          query
          (format "create temp table %s (id serial, title text)" table)

          res
          (api/query conn query)]

      (is (nil? res)))))


#_
(deftest test-client-listen-notify

  (let [capture!
        (atom [])

        fn-notification
        (fn [Message]
          (swap! capture! conj Message))

        config+
        (assoc CONFIG :fn-notification fn-notification)]

    (api/with-connection [conn config+]

      (let [res1
            (api/query conn "listen FOO")

            res2
            (api/query conn "notify FOO, 'kek-lol'")

            res3
            (api/query conn "unlisten FOO")

            res4
            (api/query conn "notify FOO, 'hello'")

            messages
            @capture!]

        (is (nil? res1))
        (is (nil? res2))
        (is (nil? res3))
        (is (nil? res4))

        (is (= 1 (count messages)))

        (is (= {:channel "foo" :message "kek-lol"}
               (-> messages
                   first
                   (dissoc :pid))))))))


#_
(deftest test-client-listen-notify-different-conns

  (let [capture!
        (atom [])

        fn-notification
        (fn [Message]
          (swap! capture! conj Message))

        config+
        (assoc CONFIG :fn-notification fn-notification)]

    (api/with-connection [conn1 CONFIG]
      (api/with-connection [conn2 config+]

        (let [pid1 (api/pid conn1)
              pid2 (api/pid conn2)]

          (api/query conn2 "listen FOO")
          (api/query conn1 "notify FOO, 'message1'")
          (api/query conn1 "notify FOO, 'message2'")

          (api/query conn2 "")

          (is (= [{:pid pid1 :channel "foo" :message "message1"}
                  {:pid pid1 :channel "foo" :message "message2"}]

                 (mapv ->map @capture!))))))))


#_
(deftest test-client-broken-query
  (api/with-connection [conn CONFIG]
    (try
      (api/query conn "selekt 1")
      (catch Exception e
        (is (= "ErrorResponse" (ex-message e)))
        (is (= {:errors
                {:severity "ERROR"
                 :verbosity "ERROR"
                 :code "42601"
                 :message "syntax error at or near \"selekt\""
                 :position "1"
                 :function "scanner_yyerror"}
                :details {:query "selekt 1"}}
               (-> e
                   ex-data
                   (update :errors
                           (fn [errors]
                             (dissoc errors :file :line))))))))))

#_

(deftest test-client-error-response

  (let [config
        (assoc CONFIG :pg-params {"pg_foobar" "111"})]

    (is (thrown? Exception
                 (api/with-connection [conn config]
                   42)))))


#_
(deftest test-client-wrong-startup-params

  (let [config
        (assoc CONFIG :pg-params {"application_name" "Clojure"})]

    (api/with-connection [conn config]
      (let [param
            (api/get-param conn "application_name")]
        (is (= "Clojure" param))))))


#_
(deftest test-client-prepare

  (api/with-connection [conn CONFIG]

    (let [query1
          "prepare foo as select $1::integer as num"

          res1
          (api/query conn query1)

          query2
          "execute foo(42)"

          res2
          (api/query conn query2)

          query3
          "deallocate foo"

          res3
          (api/query conn query3)]

      (is (nil? res1))
      (is (= [{:num 42}] res2))
      (is (nil? res3)))))


#_
(deftest test-client-cursor

  (api/with-connection [conn CONFIG]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (api/query conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2') returning *" table)

          _
          (api/query conn query2)

          query3
          (format "DECLARE cur CURSOR for select * from %s" table)]

      (api/with-tx [conn]

        (let [res3
              (api/query conn query3)

              res4
              (api/query conn "fetch next from cur")

              res5
              (api/query conn "fetch next from cur")

              res6
              (api/query conn "fetch next from cur")]

          (api/query conn "close cur")

          (is (nil? res3))

          (is (= [{:id 1 :title "test1"}] res4))
          (is (= [{:id 2 :title "test2"}] res5))
          (is (= [] res6)))))))


#_
(deftest test-client-wrong-minor-protocol

  (let [config
        (assoc CONFIG :protocol-version 196609)]

    (api/with-connection [conn config]
      (is (= [{:foo 1}]
             (api/query conn "select 1 as foo"))))))


#_
(deftest test-client-empty-select
  (api/with-connection [conn CONFIG]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (api/query conn query1)

          query2
          (format "select * from %s" table)

          res
          (api/query conn query2)]

      (is (= [] res)))))


#_
(deftest test-client-insert-result-returning
  (api/with-connection [conn CONFIG]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (api/query conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2') returning *" table)

          res
          (api/query conn query2)]

      (is (= [{:id 1 :title "test1"}
              {:id 2 :title "test2"}]
             res)))))


#_
(deftest test-client-notice-custom-function
  (let [capture!
        (atom nil)

        config
        (assoc CONFIG :fn-notice
               (fn [fields]
                 (reset! capture! fields)))]

    (api/with-connection [conn config]
      (let [res (api/query conn "ROLLBACK")]
        (is (nil? res))))

    (is (= {:severity "WARNING"
            :verbosity "WARNING"
            :code "25P01"
            :message "there is no transaction in progress"
            :file "xact.c"
            :function "UserAbortTransactionBlock"}

           (-> @capture!
               (dissoc :line))))))


#_
(deftest test-client-insert-result-no-returning
  (api/with-connection [conn CONFIG]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (api/query conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2')" table)

          res
          (api/query conn query2)]

      (is (= 2 res)))))


#_
(deftest test-client-select-fn-result
  (api/with-connection [conn CONFIG]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (api/query conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2')" table)

          _
          (api/query conn query2)

          query3
          (format "select * from %s where id = 1" table)

          res
          (api/query conn query3 {:fn-result first})]

      (is (= {:id 1 :title "test1"} res)))))


#_
(deftest test-client-delete-result
  (api/with-connection [conn CONFIG]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (api/query conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2')" table)

          _
          (api/query conn query2)

          query3
          (format "delete from %s " table)

          res
          (api/query conn query3)]

      (is (= 2 res)))))


#_
(deftest test-client-update-result
  (api/with-connection [conn CONFIG]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (api/query conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2')" table)

          _
          (api/query conn query2)

          query3
          (format "update %s set title = 'aaa'" table)

          res
          (api/query conn query3)]

      (is (= 2 res)))))


#_
(deftest test-client-mixed-result
  (api/with-connection [conn CONFIG]

    (let [table
          (gen-table)

          query
          (format
           "
create temp table %1$s (id serial, title text);
insert into %1$s (id, title) values (1, 'test1'), (2, 'test2');
insert into %1$s (id, title) values (3, 'test3') returning *;
select * from %1$s where id <> 3;
update %1$s set title = 'aaa' where id = 1;
delete from %1$s where id = 2;
drop table %1$s;
"
           table)

          res
          (api/query conn query {:fn-column str/upper-case})]

      (is (= [nil
              2
              [{"ID" 3 "TITLE" "test3"}]
              [{"ID" 1 "TITLE" "test1"}
               {"ID" 2 "TITLE" "test2"}]
              1
              1
              nil]
             res)))))


#_
(deftest test-client-truncate-result
  (api/with-connection [conn CONFIG]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (api/query conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2')" table)

          _
          (api/query conn query2)

          query3
          (format "truncate %s" table)

          res
          (api/query conn query3)]

      (is (nil? res)))))


#_
(deftest test-client-select-multi

  (api/with-connection [conn CONFIG]

    (let [res
          (api/query conn "select 1 as foo; select 2 as bar")]

      (is (= [[{:foo 1}] [{:bar 2}]] res)))))


#_
(deftest test-client-field-duplicates

  (api/with-connection [conn CONFIG]

    (let [res
          (api/query conn "select 1 as id, 2 as id")]

      (is (= [{:id_0 1 :id_1 2}] res)))))


#_
(deftest test-client-as-vectors

  (api/with-connection [conn CONFIG]

    (let [res
          (api/query conn "select 1 as id, 2 as id" {:as-vectors? true})]

      (is (= [[1 2]] res)))))


#_
(deftest test-client-as-java-maps

  (api/with-connection [conn CONFIG]

    (let [res
          (api/query conn "select 1 as id, 2 as id" {:as-java-maps? true})]

      (is (instance? java.util.HashMap (first res)))
      (is (= [{:id_0 1 :id_1 2}] res)))))


#_
(deftest test-client-json-read
  (api/with-connection [conn CONFIG]
    (let [res
          (api/query conn "select '[1, 2, 3]'::json as arr")]
      (is (= [{:arr [1 2 3]}] res)))))


#_
(deftest test-client-jsonb-read
  (api/with-connection [conn CONFIG]
    (let [res
          (api/query conn "select '{\"foo\": 123}'::jsonb as obj")]
      (is (= [{:obj {:foo 123}}] res)))))


#_
(deftest test-client-prepare-&-close-ok

  (api/with-connection [conn CONFIG]

    (let [statement
          (api/prepare conn "select 1 as foo")]

      (is (string? statement))
      (is (str/starts-with? statement "statement"))

      (let [result
            (api/close-statement conn statement)]

        (is (nil? result))))))


#_
(deftest test-with-prepare
  (api/with-connection [conn CONFIG]
    (api/with-prepare [stmt conn "select 1 as foo"]
      (is (string? stmt)))))

#_
(deftest test-execute
  (api/with-connection [conn CONFIG]
    (let [result
          (api/execute conn "select $1::integer as foo" [42])]
      (is (= [{:foo 42}] result)))))


#_
(deftest test-execute-row-limit
  (api/with-connection [conn CONFIG]

    (let [query
          "with foo as (values (1, 2), (3, 4), (5, 6)) select * from foo"

          result
          (api/execute conn query [])]

      (is (= 42 result)))))



;; test-client-json-write
;; test-client-jsonb-write
