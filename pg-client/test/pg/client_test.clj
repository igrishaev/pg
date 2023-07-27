(ns pg.client-test
  (:import
   java.time.Instant
   java.time.LocalDate
   java.time.LocalDateTime
   java.time.LocalTime
   java.time.OffsetDateTime
   java.time.OffsetTime
   java.util.ArrayList
   java.util.Date
   java.util.HashMap)
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is use-fixtures testing]]
   [pg.client :as pg]
   [pg.client.acc :as acc]
   [pg.client.conn :as conn]
   pg.json))


(def PORTS
  [10110 10120 10130 10140 10150 10160])


(def ^:dynamic *CONFIG*
  {:host "127.0.0.1"
   :port -999
   :user "test"
   :password "test"
   :database "test"})


(defn fix-multi-version [t]
  (doseq [port PORTS]
    (binding [*CONFIG* (assoc *CONFIG* :port port)]
      (testing (format "PORT %s" port)
        (t)))))


(use-fixtures :each fix-multi-version)


(defn gen-table []
  (format "table_%s" (System/nanoTime)))


(deftest test-client-tx-status

  (pg/with-connection [conn *CONFIG*]

    (is (= :I (pg/status conn)))

    (is (pg/idle? conn))

    (pg/execute conn "select 1")

    (is (= :I (pg/status conn)))

    (pg/begin conn)

    (is (= :T (pg/status conn)))

    (is (pg/in-transaction? conn))

    (try
      (pg/execute conn "selekt 1")
      (catch Exception _
        nil))

    (is (= :E (pg/status conn)))

    (is (pg/tx-error? conn))

    (pg/rollback conn)

    (is (= :I (pg/status conn)))))


(deftest test-client-conn-str-print
  (pg/with-connection [conn *CONFIG*]

    (is (= "PG connection ivan@127.0.0.1:15432/ivan"
           (str conn)))

    (is (= "PG connection ivan@127.0.0.1:15432/ivan"
           (with-out-str
             (print conn))))))


(deftest test-client-conn-equals
  (pg/with-connection [conn1 *CONFIG*]
    (pg/with-connection [conn2 *CONFIG*]
      (is (= conn1 conn1))
      (is (not= conn1 conn2)))))


(deftest test-client-ok

  (let [result
        (pg/with-connection [conn *CONFIG*]
          (pg/execute conn "select 1 as foo, 'hello' as bar"))]

    (is (= [{:foo 1 :bar "hello"}]
           result))))


(deftest test-client-query-multiple

  (let [result
        (pg/with-connection [conn *CONFIG*]
          (pg/execute conn "select 1 as foo; select 'two' as bar"))]

    (is (= [[{:foo 1}]
            [{:bar "two"}]]
           result))))


(deftest test-client-empty-query

  (let [result
        (pg/with-connection [conn *CONFIG*]
          (pg/execute conn ""))]

    (is (nil? result))))


(deftest test-client-fn-column

  (let [result
        (pg/with-connection [conn *CONFIG*]
          (pg/execute conn "select 1 as foo" nil {:fn-column str/upper-case}))]

    (is (= [{"FOO" 1}] result))))


(deftest test-client-exception-in-the-middle

  (pg/with-connection [conn *CONFIG*]

    (is (thrown?
         Exception
         (with-redefs [pg.decode.txt/-decode
                       (fn [& _]
                         (throw (new Exception "boom")))]
           (pg/execute conn "select 1 as foo"))))))


(deftest test-client-reuse-conn

  (pg/with-connection [conn *CONFIG*]

    (let [res1
          (pg/execute conn "select 1 as foo")

          res2
          (pg/execute conn "select 'hello' as bar")]

      (is (= [{:foo 1}] res1))
      (is (= [{:bar "hello"}] res2)))))


(deftest test-client-with-tx-syntax-issue
  (pg/with-connection [conn *CONFIG*]
    (pg/with-tx [conn]
      (is (map? conn)))))


(deftest test-client-with-transaction-ok

  (pg/with-connection [conn *CONFIG*]

    (let [res1
          (pg/with-tx [conn]
            (pg/execute conn "select 1 as foo" nil {:fn-result first}))

          res2
          (pg/with-tx [conn]
            (pg/execute conn "select 2 as bar" nil {:fn-result first}))]

      (is (= {:foo 1} res1))
      (is (= {:bar 2} res2)))))


(deftest test-client-with-transaction-read-only

  (pg/with-connection [conn *CONFIG*]

    (let [res1
          (pg/with-tx [conn {:read-only? true}]
            (pg/execute conn "select 1 as foo"))]

      (is (= [{:foo 1}] res1))

      (try
        (pg/with-tx [conn {:read-only? true}]
          (pg/execute conn "create temp table foo123 (id integer)"))
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

  (pg/with-connection [conn *CONFIG*]

    (let [result
          (pg/execute conn "explain analyze select 42")

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

    (pg/with-connection [conn *CONFIG*]

      (pg/execute conn (format "create table %s (id integer)" table))

      (pg/with-tx [conn {:isolation-level "serializable"}]
        (pg/execute conn (format "insert into %s values (1), (2)" table))

        (let [res1
              (pg/execute conn (format "select * from %s" table))

              res2
              (pg/with-connection [conn2 *CONFIG*]
                (pg/execute conn2 (format "select * from %s" table)))]

          (is (= [{:id 1} {:id 2}] res1))
          (is (= [] res2)))))))


(deftest test-client-with-transaction-rollback

  (let [table
        (gen-table)]

    (pg/with-connection [conn *CONFIG*]

      (pg/execute conn (format "create table %s (id integer)" table))

      (pg/with-tx [conn {:rollback? true}]
        (pg/execute conn (format "insert into %s values (1), (2)" table)))

      (let [res1
            (pg/execute conn (format "select * from %s" table))]

        (is (= [] res1))))))


(deftest test-client-create-table
  (pg/with-connection [conn *CONFIG*]

    (let [table
          (gen-table)

          query
          (format "create temp table %s (id serial, title text)" table)

          res
          (pg/execute conn query)]

      (is (nil? res)))))


(deftest test-client-listen-notify

  (let [capture!
        (atom [])

        fn-notification
        (fn [Message]
          (swap! capture! conj Message))

        config+
        (assoc *CONFIG* :fn-notification fn-notification)]

    (pg/with-connection [conn config+]

      (let [pid
            (pg/pid conn)

            res1
            (pg/execute conn "listen FOO")

            res2
            (pg/execute conn "notify FOO, 'kek-lol'")

            res3
            (pg/execute conn "unlisten FOO")

            res4
            (pg/execute conn "notify FOO, 'hello'")

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
        (assoc *CONFIG* :fn-notification fn-notification)]

    (pg/with-connection [conn1 *CONFIG*]
      (pg/with-connection [conn2 config+]

        (let [pid1 (pg/pid conn1)
              pid2 (pg/pid conn2)]

          (pg/execute conn2 "listen FOO")
          (pg/execute conn1 "notify FOO, 'message1'")
          (pg/execute conn1 "notify FOO, 'message2'")

          (pg/execute conn2 "")

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
  (pg/with-connection [conn *CONFIG*]
    (try
      (pg/execute conn "selekt 1")
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
        (assoc *CONFIG* :pg-params {"pg_foobar" "111"})]

    (is (thrown? Exception
                 (pg/with-connection [conn config]
                   42)))))


(deftest test-client-wrong-startup-params

  (let [config
        (assoc *CONFIG* :pg-params {"application_name" "Clojure"})]

    (pg/with-connection [conn config]
      (let [param
            (pg/get-parameter conn "application_name")]
        (is (= "Clojure" param))))))


(deftest test-terminate-closed
  (pg/with-connection [conn *CONFIG*]
    (pg/terminate conn)
    (is (pg/closed? conn))))


(deftest test-client-prepare

  (pg/with-connection [conn *CONFIG*]

    (let [query1
          "prepare foo as select $1::integer as num"

          res1
          (pg/execute conn query1)

          query2
          "execute foo(42)"

          res2
          (pg/execute conn query2)

          query3
          "deallocate foo"

          res3
          (pg/execute conn query3)]

      (is (nil? res1))
      (is (= [{:num 42}] res2))
      (is (nil? res3)))))


(deftest test-client-cursor

  (pg/with-connection [conn *CONFIG*]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (pg/execute conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2') returning *" table)

          _
          (pg/execute conn query2)

          query3
          (format "DECLARE cur CURSOR for select * from %s" table)]

      (pg/with-tx [conn]

        (let [res3
              (pg/execute conn query3)

              res4
              (pg/execute conn "fetch next from cur")

              res5
              (pg/execute conn "fetch next from cur")

              res6
              (pg/execute conn "fetch next from cur")]

          (pg/execute conn "close cur")

          (is (nil? res3))

          (is (= [{:id 1 :title "test1"}] res4))
          (is (= [{:id 2 :title "test2"}] res5))
          (is (= [] res6)))))))


(deftest test-client-wrong-minor-protocol

  (let [config
        (assoc *CONFIG* :protocol-version 196609)]

    (pg/with-connection [conn config]
      (is (= [{:foo 1}]
             (pg/execute conn "select 1 as foo"))))))


(deftest test-client-wrong-major-protocol

  (let [config
        (assoc *CONFIG* :protocol-version 296608)]

    (try
      (pg/with-connection [conn config]
        (pg/execute conn "select 1 as foo"))
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
  (pg/with-connection [conn *CONFIG*]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (pg/execute conn query1)

          query2
          (format "select * from %s" table)

          res
          (pg/execute conn query2)]

      (is (= [] res)))))


(deftest test-client-insert-result-returning
  (pg/with-connection [conn *CONFIG*]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (pg/execute conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2') returning *" table)

          res
          (pg/execute conn query2)]

      (is (= [{:id 1 :title "test1"}
              {:id 2 :title "test2"}]
             res)))))


(deftest test-client-notice-custom-function
  (let [capture!
        (atom nil)

        config
        (assoc *CONFIG* :fn-notice
               (fn [message]
                 (reset! capture! message)))]

    (pg/with-connection [conn config]
      (let [res (pg/execute conn "ROLLBACK")]
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
  (pg/with-connection [conn *CONFIG*]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (pg/execute conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2')" table)

          res
          (pg/execute conn query2)]

      (is (= 2 res)))))


(deftest test-client-select-fn-result
  (pg/with-connection [conn *CONFIG*]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (pg/execute conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2')" table)

          _
          (pg/execute conn query2)

          query3
          (format "select * from %s where id = 1" table)

          res
          (pg/execute conn query3 nil {:fn-result first})]

      (is (= {:id 1 :title "test1"} res)))))


(deftest test-prepare-result

  (pg/with-connection [conn *CONFIG*]

    (let [res
          (pg/prepare-statement conn "select $1::integer as foo")]

      (is (map? res))
      (is (= [:statement :RowDescription :ParameterDescription]
             (keys res))))))


(deftest test-prepare-execute

  (pg/with-connection [conn *CONFIG*]

    (pg/with-statement [stmt conn "select $1::integer as foo"]

      (let [res1
            (pg/execute-statement conn stmt [1])

            res2
            (pg/execute-statement conn stmt [2])]

        (is (= [{:foo 1}] res1))
        (is (= [{:foo 2}] res2))))))


(deftest test-prepare-execute-with-options

  (pg/with-connection [conn *CONFIG*]

    (pg/with-statement [stmt conn "select $1::integer as foo"]

      (let [res1
            (pg/execute-statement conn stmt [1] {:fn-column str/upper-case})

            res2
            (pg/execute-statement conn stmt [2] {:fn-result first})]

        (is (= [{"FOO" 1}] res1))
        (is (= {:foo 2} res2))))))


(deftest test-client-delete-result
  (pg/with-connection [conn *CONFIG*]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (pg/execute conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2')" table)

          _
          (pg/execute conn query2)

          query3
          (format "delete from %s " table)

          res
          (pg/execute conn query3)]

      (is (= 2 res)))))


(deftest test-client-update-result
  (pg/with-connection [conn *CONFIG*]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (pg/execute conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2')" table)

          _
          (pg/execute conn query2)

          query3
          (format "update %s set title = 'aaa'" table)

          res
          (pg/execute conn query3)]

      (is (= 2 res)))))


(deftest test-client-mixed-result
  (pg/with-connection [conn *CONFIG*]

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
          (pg/execute conn query nil {:fn-column str/upper-case})]

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
  (pg/with-connection [conn *CONFIG*]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (pg/execute conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2')" table)

          _
          (pg/execute conn query2)

          query3
          (format "truncate %s" table)

          res
          (pg/execute conn query3)]

      (is (nil? res)))))


(deftest test-client-select-multi

  (pg/with-connection [conn *CONFIG*]

    (let [res
          (pg/execute conn "select 1 as foo; select 2 as bar")]

      (is (= [[{:foo 1}] [{:bar 2}]] res)))))


(deftest test-client-field-duplicates

  (pg/with-connection [conn *CONFIG*]

    (let [res
          (pg/execute conn "select 1 as id, 2 as id")]

      (is (= [{:id 1 :id_1 2}] res)))))


(deftest test-client-json-read
  (pg/with-connection [conn *CONFIG*]
    (let [res
          (pg/execute conn "select '[1, 2, 3]'::json as arr")]
      (is (= [{:arr [1 2 3]}] res)))))


(deftest test-client-jsonb-read
  (pg/with-connection [conn *CONFIG*]
    (let [res
          (pg/execute conn "select '{\"foo\": 123}'::jsonb as obj")]
      (is (= [{:obj {:foo 123}}] res)))))


(deftest test-client-json-write
  (pg/with-connection [conn *CONFIG*]
    (let [res
          (pg/execute conn
                      "select $1::json as obj"
                      [{:foo 123}]
                      {:fn-result first})]
      (is (= {:obj {:foo 123}} res)))))


(deftest test-client-jsonb-write
  (pg/with-connection [conn *CONFIG*]
    (let [json
          [1 2 [true {:foo 1}]]

          res
          (pg/execute conn
                      "select $1::jsonb as obj"
                      [json]
                      {:fn-result first})]
      (is (= '{:obj (1 2 [true {:foo 1}])} res)))))


(deftest test-client-default-oid-long
  (pg/with-connection [conn *CONFIG*]
    (let [res (pg/execute conn "select $1 as foo" [42])]
      (is (= [{:foo 42}] res)))))


(deftest test-client-default-oid-uuid
  (pg/with-connection [conn *CONFIG*]
    (let [uid (random-uuid)
          res (pg/execute conn "select $1 as foo" [uid])]
      (is (= [{:foo uid}] res)))))


(deftest test-client-execute-sqlvec
  (pg/with-connection [conn *CONFIG*]
    (let [res (pg/execute conn "select $1 as foo" ["hi"])]
      (is (= [{:foo "hi"}] res)))))


(deftest test-client-execute-sqlvec-no-params
  (pg/with-connection [conn *CONFIG*]
    (let [res (pg/execute conn "select 42 as foo")]
      (is (= [{:foo 42}] res)))))


(deftest test-client-timestamptz-read
  (pg/with-connection [conn *CONFIG*]
    (let [res (pg/execute conn "select '2022-01-01 23:59:59.123+03'::timestamptz as obj")
          obj (-> res first :obj)]
      (is (instance? Instant obj))
      (is (= "2022-01-01T20:59:59.123Z" (str obj))))))


(deftest test-client-timestamptz-pass
  (pg/with-connection [conn *CONFIG*]
    (pg/with-statement [stmt conn "select $1::timestamptz as obj"]
      (let [inst
            (Instant/parse "2022-01-01T20:59:59.123456Z")
            res
            (pg/execute-statement conn stmt [inst])]
        (is (= inst (-> res first :obj)))))))


(deftest test-client-timestamp-read
  (pg/with-connection [conn *CONFIG*]
    (let [res (pg/execute conn "select '2022-01-01 23:59:59.123+03'::timestamp as obj")
          obj (-> res first :obj)]
      (is (instance? LocalDateTime obj))
      (is (= "2022-01-01T23:59:59.123" (str obj))))))


(deftest test-client-timestamp-pass
  (pg/with-connection [conn *CONFIG*]
    (pg/with-statement [stmt conn "select $1::timestamp as obj"]
      (let [inst
            (Instant/parse "2022-01-01T20:59:59.000000123Z")
            res
            (pg/execute-statement conn stmt [inst])]

        (is (= "2022-01-01T20:59:59"
               (-> res first :obj str)))))))


(deftest test-client-instant-date-read
  (pg/with-connection [conn *CONFIG*]
    (let [res (pg/execute conn "select '2022-01-01 23:59:59.123+03'::date as obj")
          obj (-> res first :obj)]
      (is (instance? LocalDate obj))
      (is (= "2022-01-01" (str obj))))))


(deftest test-client-pass-date-timestamptz
  (pg/with-connection [conn *CONFIG*]
    (let [date
          (new Date 85 11 31 23 59 59)

          res
          (pg/execute conn "select $1::timestamptz as obj" [date])

          obj
          (-> res first :obj)]

      (is (instance? Instant obj))
      (is (= "1985-12-31T20:59:59Z" (str obj))))))


(deftest test-client-date-pass-date

  (pg/with-connection [conn *CONFIG*]
    (let [date
          (new Date 85 11 31 23 59 59)

          res
          (pg/execute conn "select EXTRACT('year' from $1)" [date])]

      (is (= [{:extract 1985M}] res)))))


(deftest test-client-read-time

  (pg/with-connection [conn *CONFIG*]
    (let [res
          (pg/execute conn "select now()::time as time")

          time
          (-> res first :time)]

      (is (instance? LocalTime time)))))


(deftest test-client-pass-time

  (pg/with-connection [conn *CONFIG*]
    (let [time1
          (LocalTime/now)

          res
          (pg/execute conn "select $1::time as time" [time1])

          time2
          (-> res first :time)]

      (is (= time1 time2)))))


(deftest test-client-read-timetz

  (pg/with-connection [conn *CONFIG*]
    (let [res
          (pg/execute conn "select now()::timetz as timetz")

          timetz
          (-> res first :timetz)]

      (is (instance? OffsetTime timetz)))))


(deftest test-client-pass-timetz

  (pg/with-connection [conn *CONFIG*]
    (let [time1
          (OffsetTime/now)

          res
          (pg/execute conn "select $1::timetz as timetz" [time1])

          time2
          (-> res first :timetz)]

      (is (= time1 time2)))))


(deftest test-client-conn-with-open
  (with-open [conn (pg/connect *CONFIG*)]
    (let [res (pg/execute conn "select 1 as one")]
      (is (= [{:one 1}] res)))))


(deftest test-client-prepare-&-close-ok

  (pg/with-connection [conn *CONFIG*]

    (let [statement
          (pg/prepare-statement conn "select 1 as foo")]

      (is (map? statement))

      (let [result
            (pg/close-statement conn statement)]

        (is (nil? result))

        (try
          (pg/execute-statement conn statement)
          (is false)
          (catch Exception e
            (is (= "ErrorResponse" (ex-message e)))
            (is (re-matches
                 #"prepared statement (.+?) does not exist"
                 (-> e ex-data :error :errors :message)))))))))


(deftest test-execute-row-limit
  (pg/with-connection [conn *CONFIG*]

    (let [query
          "with foo as (values (1, 2), (3, 4), (5, 6)) select * from foo"]

      (pg/with-statement [stmt conn query]

        (let [result
              (pg/execute-statement conn stmt [] {:rows 1})]

          (is (= [{:column1 1 :column2 2}]
                 result)))))))


(deftest test-acc-as-java

  (pg/with-connection [conn *CONFIG*]

    (let [query
          "with foo (a, b) as (values (1, 2), (3, 4), (5, 6)) select * from foo"

          res
          (pg/execute conn query nil {:as acc/as-java})]

      (is (= [{:b 2 :a 1}
              {:b 4 :a 3}
              {:b 6 :a 5}]
             res))

      (is (instance? ArrayList res))
      (is (every? (fn [x]
                    (instance? HashMap x))
                  res)))))


(deftest test-acc-as-index-by

  (pg/with-connection [conn *CONFIG*]

    (let [query
          "with foo (a, b) as (values (1, 2), (3, 4), (5, 6)) select * from foo"

          res
          (pg/execute conn query nil {:as (acc/as-index-by :a)})]

      (is (= {1 {:a 1 :b 2}
              3 {:a 3 :b 4}
              5 {:a 5 :b 6}}

             res)))))


(deftest test-acc-as-group-by

  (pg/with-connection [conn *CONFIG*]

    (let [query
          "with foo (a, b) as (values (1, 2), (3, 4), (5, 6)) select * from foo"

          res
          (pg/execute conn query nil {:as (acc/as-group-by :a)})]

      (is (= {1 [{:a 1 :b 2}]
              3 [{:a 3 :b 4}]
              5 [{:a 5 :b 6}]}
             res)))))


(deftest test-acc-as-kv

  (pg/with-connection [conn *CONFIG*]

    (let [query
          "with foo (a, b) as (values (1, 2), (3, 4), (5, 6)) select * from foo"

          res
          (pg/execute conn query nil {:as (acc/as-kv :b :a)})]

      (is (= {2 1
              4 3
              6 5}
             res)))))


(deftest test-acc-as-matrix

  (pg/with-connection [conn *CONFIG*]

    (let [query
          "with foo (a, b) as (values (1, 2), (3, 4), (5, 6)) select * from foo"

          res
          (pg/execute conn query nil {:as acc/as-matrix})]

      (is (= [[1 2]
              [3 4]
              [5 6]]
             res)))))


(deftest test-conn-opt
  (pg/with-connection [conn *CONFIG*]
    (let [opt (conn/get-opt conn)]
      (is (= {:date-style "ISO, MDY"
              :time-zone "Europe/Moscow"
              :server-encoding "UTF8"
              :client-encoding "UTF8"}
             opt)))))


(deftest test-two-various-params
  (pg/with-connection [conn *CONFIG*]
    (let [res
          (pg/execute conn "select $1::int8 = $1::int4 as eq" [(int 123) 123])]
      (is (= [{:eq true}] res)))))


(deftest test-empty-select
  (pg/with-connection [conn *CONFIG*]
    (let [res (pg/execute conn "select")]
      (is (= [] res)))))


(deftest test-encode-binary-simple
  (pg/with-connection [conn (assoc *CONFIG* :binary-encode? true)]
    (let [res (pg/execute conn "select $1::integer as num" [42])]
      (is (= [{:num 42}] res)))))


(deftest test-decode-binary-simple
  (pg/with-connection [conn (assoc *CONFIG* :binary-decode? true)]
    (let [res (pg/execute conn "select $1::integer as num" [42])]
      (is (= [{:num 42}] res)))))


(deftest test-decode-binary-unsupported
  (pg/with-connection [conn (assoc *CONFIG* :binary-decode? true)]
    (let [res (pg/execute conn "select '1 year 1 second'::interval as interval" [])]
      (is (= [{:interval [0 0 0 0 0 15 66 64 0 0 0 0 0 0 0 12]}]
             (update-in res [0 :interval] vec))))))


(deftest test-decode-text-unsupported
  (pg/with-connection [conn *CONFIG*]
    (let [res (pg/execute conn "select '1 year 1 second'::interval as interval")]
      (is (= [{:interval "1 year 00:00:01"}] res)))))


(deftest test-decode-binary-text
  (pg/with-connection [conn (assoc *CONFIG* :binary-decode? true)]
    (let [res (pg/execute conn "select 'hello'::text as text" [])]
      (is (= [{:text "hello"}] res)))))


(deftest test-decode-binary-varchar
  (pg/with-connection [conn (assoc *CONFIG* :binary-decode? true)]
    (let [res (pg/execute conn "select 'hello'::varchar as text" [])]
      (is (= [{:text "hello"}] res)))))


(deftest test-decode-binary-bpchar
  (pg/with-connection [conn (assoc *CONFIG*
                                   :binary-encode? true
                                   :binary-decode? true)]
    (let [res (pg/execute conn "select $1::char as char" ["ё"])]
      (is (= [{:char \ё}] res)))))


(deftest test-decode-oid
  (pg/with-connection [conn *CONFIG*]
    (let [res (pg/execute conn "select $1::oid as oid" [42])]
      (is (= [{:oid 42}] res)))))


(deftest test-decode-oid-binary
  (pg/with-connection [conn (assoc *CONFIG*
                                   :binary-encode? true
                                   :binary-decode? true)]
    (let [res (pg/execute conn "select $1::oid as oid" [42])]
      (is (= [{:oid 42}] res)))))


(deftest test-uuid-text
  (pg/with-connection [conn *CONFIG*]
    (let [uuid
          (random-uuid)
          res
          (pg/execute conn "select $1 as uuid" [uuid])]
      (is (= [{:uuid uuid}] res)))))


(deftest test-uuid-bin
  (pg/with-connection [conn (assoc *CONFIG*
                                   :binary-encode? true
                                   :binary-decode? true)]
    (let [uuid
          (random-uuid)
          res
          (pg/execute conn "select $1 as uuid" [uuid])]
      (is (= [{:uuid uuid}] res)))))


(deftest test-time-bin-read
  (pg/with-connection [conn (assoc *CONFIG*
                                   :binary-encode? true
                                   :binary-decode? true)]
    (let [res
          (pg/execute conn "select '12:01:59.123456789+03'::time as time;" [])
          time
          (-> res first :time)]
      (is (instance? LocalTime time))
      (is (= "12:01:59.123457" (str time))))))


(deftest test-timetz-bin-read
  (pg/with-connection [conn (assoc *CONFIG*
                                   :binary-encode? true
                                   :binary-decode? true)]
    (let [res
          (pg/execute conn "select '12:01:59.123456789+03'::timetz as timetz;" [])
          timetz
          (-> res first :timetz)]
      (is (instance? OffsetTime timetz))
      (is (= "12:01:59.123457+03:00" (str timetz))))))


(deftest test-timestamp-bin-read
  (pg/with-connection [conn (assoc *CONFIG*
                                   :binary-encode? true
                                   :binary-decode? true)]
    (let [res
          (pg/execute conn "select '2022-01-01 12:01:59.123456789+03'::timestamp as ts;" [])
          ts
          (-> res first :ts)]
      (is (instance? LocalDateTime ts))
      (is (= "2022-01-01T12:01:59.123457" (str ts))))))


(deftest test-timestamptz-bin-read
  (pg/with-connection [conn (assoc *CONFIG*
                                   :binary-encode? true
                                   :binary-decode? true)]
    (let [res
          (pg/execute conn "select '2022-01-01 12:01:59.123456789+03'::timestamptz as tstz;" [])
          tstz
          (-> res first :tstz)]
      (is (instance? OffsetDateTime tstz))
      (is (= "2022-01-01T09:01:59.123457Z" (str tstz))))))


(deftest test-date-bin-read
  (pg/with-connection [conn (assoc *CONFIG*
                                   :binary-encode? true
                                   :binary-decode? true)]
    (let [res
          (pg/execute conn "select '2022-01-01 12:01:59.123456789+03'::date as date;" [])
          date
          (-> res first :date)]
      (is (instance? LocalDate date))
      (is (= "2022-01-01" (str date))))))


(deftest test-pass-zoned-time-timetz-bin
  (pg/with-connection [conn (assoc *CONFIG*
                                   :binary-encode? true
                                   :binary-decode? true)]
    (let [x1
          (OffsetTime/now)

          res
          (pg/execute conn "select $1 as x;" [x1])

          x2
          (-> res first :x)]

      (is (instance? OffsetTime x2))
      (is (= x1 x2)))))


(deftest test-pass-local-time-time-bin
  (pg/with-connection [conn (assoc *CONFIG*
                                   :binary-encode? true
                                   :binary-decode? true)]
    (let [x1
          (LocalTime/now)

          res
          (pg/execute conn "select $1 as x;" [x1])

          x2
          (-> res first :x)]

      (is (instance? LocalTime x2))
      (is (= x1 x2)))))


(deftest test-pass-instant-timestamptz-bin
  (pg/with-connection [conn (assoc *CONFIG*
                                   :binary-encode? true
                                   :binary-decode? true)]
    (let [x1
          (Instant/now)

          res
          (pg/execute conn "select $1 as x;" [x1])

          ^OffsetDateTime x2
          (-> res first :x)]

      (is (= x1 (.toInstant x2))))))


(deftest test-pass-instant-timestamp-bin
  (pg/with-connection [conn (assoc *CONFIG*
                                   :binary-encode? true
                                   :binary-decode? true)]
    (let [x1
          (Instant/parse "2023-07-25T12:36:15.981981Z")

          res
          (pg/execute conn "select $1::timestamp as x" [x1])

          x2
          (-> res first :x)]

      (is (= (LocalDateTime/parse "2023-07-25T12:36:15.981981")
             x2)))))


(deftest test-pass-date-timestamp-bin
  (pg/with-connection [conn (assoc *CONFIG*
                                   :binary-encode? true
                                   :binary-decode? true)]
    (let [x1
          (new Date 123123123123123)
          ;; 5871-08-14T03:32:03.123-00:00

          res
          (pg/execute conn "select $1::timestamp as x" [x1])

          x2
          (-> res first :x)]

      (is (= "5871-08-14T03:32:03.123" (str x2))))))


(deftest test-pass-date-timestamptz-bin
  (pg/with-connection [conn (assoc *CONFIG*
                                   :binary-encode? true
                                   :binary-decode? true)]
    (let [x1
          (new Date 123123123123123)
          ;; 5871-08-14T03:32:03.123-00:00

          res
          (pg/execute conn "select $1::timestamptz as x" [x1])

          x2
          (-> res first :x)]

      (is (= "5871-08-14T03:32:03.123Z" (str x2))))))


(deftest test-read-numeric-binary
  (pg/with-connection [conn (assoc *CONFIG*
                                   :binary-encode? false
                                   :binary-decode? true)]
    (let [res
          (pg/execute conn "select -123.456::numeric as x" [])

          x2
          (-> res first :x)]

      (is (= "5871-08-14T03:32:03.123Z" (str x2))))))
