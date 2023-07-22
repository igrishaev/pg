(ns pg.client.client-test
  (:import
   java.time.Instant
   java.time.LocalTime
   java.time.LocalDateTime
   java.time.OffsetTime
   java.time.LocalDate
   java.util.ArrayList
   java.util.Date
   java.util.HashMap)
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]
   [pg.client.acc :as acc]
   [pg.client.api :as api]
   [pg.client.conn :as conn]
   pg.json))


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

    (is (api/idle? conn))

    (api/execute conn "select 1")

    (is (= :I (api/status conn)))

    (api/begin conn)

    (is (= :T (api/status conn)))

    (is (api/in-transaction? conn))

    (try
      (api/execute conn "selekt 1")
      (catch Exception _
        nil))

    (is (= :E (api/status conn)))

    (is (api/tx-error? conn))

    (api/rollback conn)

    (is (= :I (api/status conn)))))


(deftest test-client-conn-str-print
  (api/with-connection [conn CONFIG]

    (is (= "PG connection ivan@127.0.0.1:15432/ivan"
           (str conn)))

    (is (= "PG connection ivan@127.0.0.1:15432/ivan"
           (with-out-str
             (print conn))))))


(deftest test-client-conn-equals
  (api/with-connection [conn1 CONFIG]
    (api/with-connection [conn2 CONFIG]
      (is (= conn1 conn1))
      (is (not= conn1 conn2)))))


(deftest test-client-ok

  (let [result
        (api/with-connection [conn CONFIG]
          (api/execute conn "select 1 as foo, 'hello' as bar"))]

    (is (= [{:foo 1 :bar "hello"}]
           result))))


(deftest test-client-query-multiple

  (let [result
        (api/with-connection [conn CONFIG]
          (api/execute conn "select 1 as foo; select 'two' as bar"))]

    (is (= [[{:foo 1}]
            [{:bar "two"}]]
           result))))


(deftest test-client-empty-query

  (let [result
        (api/with-connection [conn CONFIG]
          (api/execute conn ""))]

    (is (nil? result))))


(deftest test-client-fn-column

  (let [result
        (api/with-connection [conn CONFIG]
          (api/execute conn "select 1 as foo" nil {:fn-column str/upper-case}))]

    (is (= [{"FOO" 1}] result))))


(deftest test-client-exception-in-the-middle

  (api/with-connection [conn CONFIG]

    (is (thrown?
         Exception
         (with-redefs [pg.decode.txt/-decode
                       (fn [& _]
                         (throw (new Exception "boom")))]
           (api/execute conn "select 1 as foo"))))))


(deftest test-client-reuse-conn

  (api/with-connection [conn CONFIG]

    (let [res1
          (api/execute conn "select 1 as foo")

          res2
          (api/execute conn "select 'hello' as bar")]

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
            (api/execute conn "select 1 as foo" nil {:fn-result first}))

          res2
          (api/with-tx [conn]
            (api/execute conn "select 2 as bar" nil {:fn-result first}))]

      (is (= {:foo 1} res1))
      (is (= {:bar 2} res2)))))


(deftest test-client-with-transaction-read-only

  (api/with-connection [conn CONFIG]

    (let [res1
          (api/with-tx [conn {:read-only? true}]
            (api/execute conn "select 1 as foo"))]

      (is (= [{:foo 1}] res1))

      (try
        (api/with-tx [conn {:read-only? true}]
          (api/execute conn "create temp table foo123 (id integer)"))
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


(deftest test-exeplain-alalyze

  (api/with-connection [conn CONFIG]

    (let [result
          (api/execute conn "explain analyze select 42")

          lines
          (mapv (keyword "QUERY PLAN") result)

          prefixes
          (for [line lines]
            (-> line (str/split #"\s") first))]

      (is (= ["Result" "Planning" "Execution"]
             prefixes)))))


(deftest test-client-with-transaction-iso-level

  (let [table
        (gen-table)]

    (api/with-connection [conn CONFIG]

      (api/execute conn (format "create table %s (id integer)" table))

      (api/with-tx [conn {:isolation-level "serializable"}]
        (api/execute conn (format "insert into %s values (1), (2)" table))

        (let [res1
              (api/execute conn (format "select * from %s" table))

              res2
              (api/with-connection [conn2 CONFIG]
                (api/execute conn2 (format "select * from %s" table)))]

          (is (= [{:id 1} {:id 2}] res1))
          (is (= [] res2)))))))


(deftest test-client-with-transaction-rollback

  (let [table
        (gen-table)]

    (api/with-connection [conn CONFIG]

      (api/execute conn (format "create table %s (id integer)" table))

      (api/with-tx [conn {:rollback? true}]
        (api/execute conn (format "insert into %s values (1), (2)" table)))

      (let [res1
            (api/execute conn (format "select * from %s" table))]

        (is (= [] res1))))))


(deftest test-client-create-table
  (api/with-connection [conn CONFIG]

    (let [table
          (gen-table)

          query
          (format "create temp table %s (id serial, title text)" table)

          res
          (api/execute conn query)]

      (is (nil? res)))))


(deftest test-client-listen-notify

  (let [capture!
        (atom [])

        fn-notification
        (fn [Message]
          (swap! capture! conj Message))

        config+
        (assoc CONFIG :fn-notification fn-notification)]

    (api/with-connection [conn config+]

      (let [pid
            (api/pid conn)

            res1
            (api/execute conn "listen FOO")

            res2
            (api/execute conn "notify FOO, 'kek-lol'")

            res3
            (api/execute conn "unlisten FOO")

            res4
            (api/execute conn "notify FOO, 'hello'")

            messages
            @capture!

            [message]
            messages]

        (is (nil? res1))
        (is (nil? res2))
        (is (nil? res3))
        (is (nil? res4))

        (is (= 1 (count messages)))

        (is (= {:msg :NotificationResponse
                :pid pid
                :channel "foo"
                :message "kek-lol"}
               message))))))


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

          (api/execute conn2 "listen FOO")
          (api/execute conn1 "notify FOO, 'message1'")
          (api/execute conn1 "notify FOO, 'message2'")

          (api/execute conn2 "")

          (is (= [{:msg :NotificationResponse,
                   :pid pid1
                   :channel "foo"
                   :message "message1"}

                  {:msg :NotificationResponse,
                   :pid pid1
                   :channel "foo"
                   :message "message2"}]

               @capture!)))))))


(deftest test-client-broken-query
  (api/with-connection [conn CONFIG]
    (try
      (api/execute conn "selekt 1")
      (is false "must have been an error")
      (catch Exception e
        (is (= "ErrorResponse" (ex-message e)))
        (is (= {:error
                {:msg :ErrorResponse
                 :errors
                 {:severity "ERROR"
                  :verbosity "ERROR"
                  :code "42601"
                  :message "syntax error at or near \"selekt\""
                  :position "1"
                  :function "scanner_yyerror"}}}
               (-> e
                   (ex-data)
                   (update-in [:error :errors]
                              dissoc :file :line))))))))


(deftest test-client-error-response

  (let [config
        (assoc CONFIG :pg-params {"pg_foobar" "111"})]

    (is (thrown? Exception
                 (api/with-connection [conn config]
                   42)))))


(deftest test-client-wrong-startup-params

  (let [config
        (assoc CONFIG :pg-params {"application_name" "Clojure"})]

    (api/with-connection [conn config]
      (let [param
            (api/get-parameter conn "application_name")]
        (is (= "Clojure" param))))))


(deftest test-terminate-closed
  (api/with-connection [conn CONFIG]
    (api/terminate conn)
    (is (api/closed? conn))))


(deftest test-client-prepare

  (api/with-connection [conn CONFIG]

    (let [query1
          "prepare foo as select $1::integer as num"

          res1
          (api/execute conn query1)

          query2
          "execute foo(42)"

          res2
          (api/execute conn query2)

          query3
          "deallocate foo"

          res3
          (api/execute conn query3)]

      (is (nil? res1))
      (is (= [{:num 42}] res2))
      (is (nil? res3)))))


(deftest test-client-cursor

  (api/with-connection [conn CONFIG]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (api/execute conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2') returning *" table)

          _
          (api/execute conn query2)

          query3
          (format "DECLARE cur CURSOR for select * from %s" table)]

      (api/with-tx [conn]

        (let [res3
              (api/execute conn query3)

              res4
              (api/execute conn "fetch next from cur")

              res5
              (api/execute conn "fetch next from cur")

              res6
              (api/execute conn "fetch next from cur")]

          (api/execute conn "close cur")

          (is (nil? res3))

          (is (= [{:id 1 :title "test1"}] res4))
          (is (= [{:id 2 :title "test2"}] res5))
          (is (= [] res6)))))))


(deftest test-client-wrong-minor-protocol

  (let [config
        (assoc CONFIG :protocol-version 196609)]

    (api/with-connection [conn config]
      (is (= [{:foo 1}]
             (api/execute conn "select 1 as foo"))))))


(deftest test-client-wrong-major-protocol

  (let [config
        (assoc CONFIG :protocol-version 296608)]

    (try
      (api/with-connection [conn config]
        (api/execute conn "select 1 as foo"))
      (is false)
      (catch Exception e
        (is (= "ErrorResponse" (ex-message e)))
        (is (= {:error
                {:msg :ErrorResponse
                 :errors
                 {:severity "FATAL"
                  :verbosity "FATAL"
                  :code "0A000"
                  :message "unsupported frontend protocol 4.34464: server supports 3.0 to 3.0"
                  :function "ProcessStartupPacket"}}}
               (-> e
                   (ex-data)
                   (update-in [:error :errors] dissoc :file :line))))))))


(deftest test-client-empty-select
  (api/with-connection [conn CONFIG]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (api/execute conn query1)

          query2
          (format "select * from %s" table)

          res
          (api/execute conn query2)]

      (is (= [] res)))))


(deftest test-client-insert-result-returning
  (api/with-connection [conn CONFIG]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (api/execute conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2') returning *" table)

          res
          (api/execute conn query2)]

      (is (= [{:id 1 :title "test1"}
              {:id 2 :title "test2"}]
             res)))))


(deftest test-client-notice-custom-function
  (let [capture!
        (atom nil)

        config
        (assoc CONFIG :fn-notice
               (fn [message]
                 (reset! capture! message)))]

    (api/with-connection [conn config]
      (let [res (api/execute conn "ROLLBACK")]
        (is (nil? res))))

    (is (= {:msg :NoticeResponse
            :fields
            {:severity "WARNING"
             :verbosity "WARNING"
             :code "25P01"
             :message "there is no transaction in progress"
             :function "UserAbortTransactionBlock"}}
           (-> capture!
               (deref)
               (update :fields dissoc :line :file))))))


(deftest test-client-insert-result-no-returning
  (api/with-connection [conn CONFIG]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (api/execute conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2')" table)

          res
          (api/execute conn query2)]

      (is (= 2 res)))))


(deftest test-client-select-fn-result
  (api/with-connection [conn CONFIG]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (api/execute conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2')" table)

          _
          (api/execute conn query2)

          query3
          (format "select * from %s where id = 1" table)

          res
          (api/execute conn query3 nil {:fn-result first})]

      (is (= {:id 1 :title "test1"} res)))))


(deftest test-prepare-result

  (api/with-connection [conn CONFIG]

    (let [res
          (api/prepare-statement conn "select $1::integer as foo")]

      (is (map? res))
      (is (= [:statement :RowDescription :ParameterDescription]
             (keys res))))))


(deftest test-prepare-execute

  (api/with-connection [conn CONFIG]

    (api/with-statement [stmt conn "select $1::integer as foo"]

      (let [res1
            (api/execute-statement conn stmt [1])

            res2
            (api/execute-statement conn stmt [2])]

        (is (= [{:foo 1}] res1))
        (is (= [{:foo 2}] res2))))))


(deftest test-prepare-execute-with-options

  (api/with-connection [conn CONFIG]

    (api/with-statement [stmt conn "select $1::integer as foo"]

      (let [res1
            (api/execute-statement conn stmt [1] {:fn-column str/upper-case})

            res2
            (api/execute-statement conn stmt [2] {:fn-result first})]

        (is (= [{"FOO" 1}] res1))
        (is (= {:foo 2} res2))))))


(deftest test-client-delete-result
  (api/with-connection [conn CONFIG]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (api/execute conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2')" table)

          _
          (api/execute conn query2)

          query3
          (format "delete from %s " table)

          res
          (api/execute conn query3)]

      (is (= 2 res)))))


(deftest test-client-update-result
  (api/with-connection [conn CONFIG]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (api/execute conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2')" table)

          _
          (api/execute conn query2)

          query3
          (format "update %s set title = 'aaa'" table)

          res
          (api/execute conn query3)]

      (is (= 2 res)))))


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
          (api/execute conn query nil {:fn-column str/upper-case})]

      (is (= [nil
              2
              [{"ID" 3 "TITLE" "test3"}]
              [{"ID" 1 "TITLE" "test1"}
               {"ID" 2 "TITLE" "test2"}]
              1
              1
              nil]
             res)))))


(deftest test-client-truncate-result
  (api/with-connection [conn CONFIG]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (api/execute conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2')" table)

          _
          (api/execute conn query2)

          query3
          (format "truncate %s" table)

          res
          (api/execute conn query3)]

      (is (nil? res)))))


(deftest test-client-select-multi

  (api/with-connection [conn CONFIG]

    (let [res
          (api/execute conn "select 1 as foo; select 2 as bar")]

      (is (= [[{:foo 1}] [{:bar 2}]] res)))))


(deftest test-client-field-duplicates

  (api/with-connection [conn CONFIG]

    (let [res
          (api/execute conn "select 1 as id, 2 as id")]

      (is (= [{:id 1 :id_1 2}] res)))))


(deftest test-client-json-read
  (api/with-connection [conn CONFIG]
    (let [res
          (api/execute conn "select '[1, 2, 3]'::json as arr")]
      (is (= [{:arr [1 2 3]}] res)))))


(deftest test-client-jsonb-read
  (api/with-connection [conn CONFIG]
    (let [res
          (api/execute conn "select '{\"foo\": 123}'::jsonb as obj")]
      (is (= [{:obj {:foo 123}}] res)))))


(deftest test-client-json-write
  (api/with-connection [conn CONFIG]
    (let [res
          (api/execute conn
                       "select $1::json as obj"
                       [{:foo 123}]
                       {:fn-result first})]
      (is (= {:obj {:foo 123}} res)))))


(deftest test-client-jsonb-write
  (api/with-connection [conn CONFIG]
    (let [json
          [1 2 [true {:foo 1}]]

          res
          (api/execute conn
                       "select $1::jsonb as obj"
                       [json]
                       {:fn-result first})]
      (is (= '{:obj (1 2 [true {:foo 1}])} res)))))


(deftest test-client-default-oid-long
  (api/with-connection [conn CONFIG]
    (let [res (api/execute conn "select $1 as foo" [42])]
      (is (= [{:foo 42}] res)))))


(deftest test-client-default-oid-uuid
  (api/with-connection [conn CONFIG]
    (let [uid (random-uuid)
          res (api/execute conn "select $1 as foo" [uid])]
      (is (= [{:foo uid}] res)))))


(deftest test-client-execute-sqlvec
  (api/with-connection [conn CONFIG]
    (let [res (api/execute conn "select $1 as foo" ["hi"])]
      (is (= [{:foo "hi"}] res)))))


(deftest test-client-execute-sqlvec-no-params
  (api/with-connection [conn CONFIG]
    (let [res (api/execute conn "select 42 as foo")]
      (is (= [{:foo 42}] res)))))


(deftest test-client-timestamptz-read
  (api/with-connection [conn CONFIG]
    (let [res (api/execute conn "select '2022-01-01 23:59:59.123+03'::timestamptz as obj")
          obj (-> res first :obj)]
      (is (instance? Instant obj))
      (is (= "2022-01-01T20:59:59.123Z" (str obj))))))


(deftest test-client-timestamptz-pass
  (api/with-connection [conn CONFIG]
    (api/with-statement [stmt conn "select $1::timestamptz as obj"]
      (let [inst
            (Instant/parse "2022-01-01T20:59:59.123456Z")
            res
            (api/execute-statement conn stmt [inst])]
        (is (= inst (-> res first :obj)))))))


(deftest test-client-timestamp-read
  (api/with-connection [conn CONFIG]
    (let [res (api/execute conn "select '2022-01-01 23:59:59.123+03'::timestamp as obj")
          obj (-> res first :obj)]
      (is (instance? LocalDateTime obj))
      (is (= "2022-01-01T23:59:59.123" (str obj))))))


(deftest test-client-timestamp-pass
  (api/with-connection [conn CONFIG]
    (api/with-statement [stmt conn "select $1::timestamp as obj"]
      (let [inst
            (Instant/parse "2022-01-01T20:59:59.000000123Z")
            res
            (api/execute-statement conn stmt [inst])]

        (is (= "2022-01-01T20:59:59"
               (-> res first :obj str)))))))


(deftest test-client-instant-date-read
  (api/with-connection [conn CONFIG]
    (let [res (api/execute conn "select '2022-01-01 23:59:59.123+03'::date as obj")
          obj (-> res first :obj)]
      (is (instance? LocalDate obj))
      (is (= "2022-01-01" (str obj))))))


(deftest test-client-pass-date-timestamptz
  (api/with-connection [conn CONFIG]
    (let [date
          (new Date 85 11 31 23 59 59)

          res
          (api/execute conn "select $1::timestamptz as obj" [date])

          obj
          (-> res first :obj)]

      (is (instance? Instant obj))
      (is (= "1985-12-31T20:59:59Z" (str obj))))))


(deftest test-client-date-pass-date

  (api/with-connection [conn CONFIG]
    (let [date
          (new Date 85 11 31 23 59 59)

          res
          (api/execute conn "select EXTRACT('year' from $1)" [date])]

      (is (= [{:extract 1985M}] res)))))


(deftest test-client-read-time

  (api/with-connection [conn CONFIG]
    (let [res
          (api/execute conn "select now()::time as time")

          time
          (-> res first :time)]

      (is (instance? LocalTime time)))))


(deftest test-client-pass-time

  (api/with-connection [conn CONFIG]
    (let [time1
          (LocalTime/now)

          res
          (api/execute conn "select $1::time as time" [time1])

          time2
          (-> res first :time)]

      (is (= time1 time2)))))


(deftest test-client-read-timetz

  (api/with-connection [conn CONFIG]
    (let [res
          (api/execute conn "select now()::timetz as timetz")

          timetz
          (-> res first :timetz)]

      (is (instance? OffsetTime timetz)))))


(deftest test-client-pass-timetz

  (api/with-connection [conn CONFIG]
    (let [time1
          (OffsetTime/now)

          res
          (api/execute conn "select $1::timetz as timetz" [time1])

          time2
          (-> res first :timetz)]

      (is (= time1 time2)))))


(deftest test-client-conn-with-open
  (with-open [conn (api/connect CONFIG)]
    (let [res (api/execute conn "select 1 as one")]
      (is (= [{:one 1}] res)))))


(deftest test-client-prepare-&-close-ok

  (api/with-connection [conn CONFIG]

    (let [statement
          (api/prepare-statement conn "select 1 as foo")]

      (is (map? statement))

      (let [result
            (api/close-statement conn statement)]

        (is (nil? result))

        (try
          (api/execute-statement conn statement)
          (is false)
          (catch Exception e
            (is (= "ErrorResponse" (ex-message e)))
            (is (re-matches
                 #"prepared statement (.+?) does not exist"
                 (-> e ex-data :error :errors :message)))))))))


(deftest test-execute-row-limit
  (api/with-connection [conn CONFIG]

    (let [query
          "with foo as (values (1, 2), (3, 4), (5, 6)) select * from foo"]

      (api/with-statement [stmt conn query]

        (let [result
              (api/execute-statement conn stmt [] {:rows 1})]

          (is (= [{:column1 1 :column2 2}]
                 result)))))))


(deftest test-acc-as-java

  (api/with-connection [conn CONFIG]

    (let [query
          "with foo (a, b) as (values (1, 2), (3, 4), (5, 6)) select * from foo"

          res
          (api/execute conn query nil {:as acc/as-java})]

      (is (= [{:b 2 :a 1}
              {:b 4 :a 3}
              {:b 6 :a 5}]
             res))

      (is (instance? ArrayList res))
      (is (every? (fn [x]
                    (instance? HashMap x))
                  res)))))


(deftest test-acc-as-index-by

  (api/with-connection [conn CONFIG]

    (let [query
          "with foo (a, b) as (values (1, 2), (3, 4), (5, 6)) select * from foo"

          res
          (api/execute conn query nil {:as (acc/as-index-by :a)})]

      (is (= {1 {:a 1 :b 2}
              3 {:a 3 :b 4}
              5 {:a 5 :b 6}}

           res)))))


(deftest test-acc-as-group-by

  (api/with-connection [conn CONFIG]

    (let [query
          "with foo (a, b) as (values (1, 2), (3, 4), (5, 6)) select * from foo"

          res
          (api/execute conn query nil {:as (acc/as-group-by :a)})]

      (is (= {1 [{:a 1 :b 2}]
              3 [{:a 3 :b 4}]
              5 [{:a 5 :b 6}]}
             res)))))


(deftest test-acc-as-kv

  (api/with-connection [conn CONFIG]

    (let [query
          "with foo (a, b) as (values (1, 2), (3, 4), (5, 6)) select * from foo"

          res
          (api/execute conn query nil {:as (acc/as-kv :b :a)})]

      (is (= {2 1
              4 3
              6 5}
             res)))))


(deftest test-acc-as-matrix

  (api/with-connection [conn CONFIG]

    (let [query
          "with foo (a, b) as (values (1, 2), (3, 4), (5, 6)) select * from foo"

          res
          (api/execute conn query nil {:as acc/as-matrix})]

      (is (= [[1 2]
              [3 4]
              [5 6]]
             res)))))


(deftest test-conn-opt
  (api/with-connection [conn CONFIG]
    (let [opt (conn/get-opt conn)]
      (is (= {:date-style "ISO, MDY"
              :time-zone "Europe/Moscow"
              :server-encoding "UTF8"
              :client-encoding "UTF8"}
             opt)))))


(deftest test-two-various-params
  (api/with-connection [conn CONFIG]
    (let [res
          (api/execute conn "select $1::int8 = $1::int4 as eq" [(int 123) 123])]
      (is (= [{:eq true}] res)))))


(deftest test-empty-select
  (api/with-connection [conn CONFIG]
    (let [res (api/execute conn "select")]
      (is (= [] res)))))
