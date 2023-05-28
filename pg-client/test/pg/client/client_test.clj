(ns pg.client.client-test
  (:require
   [pg.client :as client]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]))


(def CONFIG
  {:host "127.0.0.1"
   :port 15432
   :user "ivan"
   :password "ivan"
   :database "ivan"})


(deftest test-client-ok

  (let [result
        (client/with-connection [conn CONFIG]
          (client/query conn "select 1 as foo, 'hello' as bar"))]

    (is (= [{:foo 1 :bar "hello"}]
           result))))


(deftest test-client-tx-status

  (client/with-connection [conn CONFIG]

    (is (= :I (client/tx-state conn)))

    (client/query conn "select 1")

    (is (= :I (client/tx-state conn)))

    (client/begin conn)

    (is (= :T (client/tx-state conn)))

    (try
      (client/query conn "selekt 1")
      (catch Exception _
        nil))

    (is (= :E (client/tx-state conn)))

    (client/rollback conn)

    (is (= :I (client/tx-state conn)))))


(deftest test-client-empty-query

  (let [result
        (client/with-connection [conn CONFIG]
          (client/query conn ""))]

    (is (nil? result))))


(deftest test-client-fn-column

  (let [result
        (client/with-connection [conn CONFIG]
          (client/query conn "select 1 as foo" {:fn-column str/upper-case}))]

    (is (= [{"FOO" 1}] result))))


(deftest test-client-exception-in-the-middle

  (client/with-connection [conn CONFIG]

    (is (thrown?
         Exception
         (with-redefs [pg.decode.txt/-decode
                       (fn [& _]
                         (throw (new Exception "boom")))]
           (client/query conn "select 1 as foo"))))

    (let [result
          (client/query conn "select 2 as bar")]

      (is (= [{:bar 2}] result)))))


(deftest test-client-reuse-conn

  (client/with-connection [conn CONFIG]

    (let [res1
          (client/query conn "select 1 as foo")
          res2
          (client/query conn "select 'hello' as bar")]

      (is (= [{:foo 1}] res1))
      (is (= [{:bar "hello"}] res2)))))


(deftest test-client-with-transaction-ok

  (client/with-connection [conn CONFIG]

    (let [res1
          (client/with-tx [conn]
            (client/query conn "select 1 as foo" {:fn-result first}))

          res2
          (client/with-tx [conn]
            (client/query conn "select 2 as bar" {:fn-result first}))]

      (is (= {:foo 1} res1))
      (is (= {:bar 2} res2)))))


(deftest test-client-create-table
  (client/with-connection [conn CONFIG]

    (let [table
          (str (gensym "table"))

          query
          (format "create temp table %s (id serial, title text)" table)

          res
          (client/query conn query)]

      (is (nil? res)))))


(deftest test-client-listen-notify

  (let [capture!
        (atom [])

        fn-notification
        (fn [Message]
          (swap! capture! conj Message))

        config+
        (assoc CONFIG :fn-notification fn-notification)]

    (client/with-connection [conn config+]

      (let [res1
            (client/query conn "listen FOO")

            res2
            (client/query conn "notify FOO, 'kek-lol'")

            res3
            (client/query conn "unlisten FOO")

            res4
            (client/query conn "notify FOO, 'hello'")

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


(deftest test-client-broken-query
  (client/with-connection [conn CONFIG]
    (try
      (client/query conn "selekt 1")
      (catch Exception e
        (is (= "ErrorResponse" (ex-message e)))
        (is (= {:S "ERROR"
                :V "ERROR"
                :C "42601"
                :M "syntax error at or near \"selekt\""
                :P "1"
                :R "scanner_yyerror"}
               (-> e
                   ex-data
                   :errors
                   (dissoc :F :L))))))))


(deftest test-client-error-response

  (let [config
        (assoc CONFIG :pg-params {"pg_foobar" "111"})]

    (is (thrown? Exception
                 (client/with-connection [conn config]
                   42)))))


(deftest test-client-wrong-startup-params

  (let [config
        (assoc CONFIG :pg-params {"application_name" "Clojure"})]

    (client/with-connection [conn config]
      (let [param
            (client/get-param conn "application_name")]
        (is (= "Clojure" param))))))


(deftest test-client-prepare

  (client/with-connection [conn CONFIG]

    (let [query1
          "prepare foo as select $1::integer as num"

          res1
          (client/query conn query1)

          query2
          "execute foo(42)"

          res2
          (client/query conn query2)

          query3
          "deallocate foo"

          res3
          (client/query conn query3)]

      (is (nil? res1))
      (is (= [{:num 42}] res2))
      (is (nil? res3)))))


(deftest test-client-cursor

  (client/with-connection [conn CONFIG]

    (let [table
          (str (gensym "table"))

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (client/query conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2') returning *" table)

          _
          (client/query conn query2)

          query3
          (format "DECLARE cur CURSOR for select * from %s" table)]

      (client/with-tx [conn]

        (let [res3
              (client/query conn query3)

              res4
              (client/query conn "fetch next from cur")

              res5
              (client/query conn "fetch next from cur")

              res6
              (client/query conn "fetch next from cur")]

          (client/query conn "close cur")

          (is (nil? res3))

          (is (= [{:id 1 :title "test1"}] res4))
          (is (= [{:id 2 :title "test2"}] res5))
          (is (= [] res6)))))))


(deftest test-client-wrong-minor-protocol

  (let [config
        (assoc CONFIG :protocol-version 196609)]

    (client/with-connection [conn config]
      (is (= [{:foo 1}]
             (client/query conn "select 1 as foo"))))))


(deftest test-client-empty-select
  (client/with-connection [conn CONFIG]

    (let [table
          (str (gensym "table"))

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (client/query conn query1)

          query2
          (format "select * from %s" table)

          res
          (client/query conn query2)]

      (is (= [] res)))))


(deftest test-client-insert-result-returning
  (client/with-connection [conn CONFIG]

    (let [table
          (str (gensym "table"))

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (client/query conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2') returning *" table)

          res
          (client/query conn query2)]

      (is (= [{:id 1 :title "test1"}
              {:id 2 :title "test2"}]
             res)))))


(deftest test-client-notice-custom-function
  (let [capture!
        (atom nil)

        config
        (assoc CONFIG :fn-notice
               (fn [fields]
                 (reset! capture! fields)))]

    (client/with-connection [conn config]
      (let [res (client/query conn "ROLLBACK")]
        (is (nil? res))))

    (is (= {:S "WARNING"
            :V "WARNING"
            :C "25P01"
            :M "there is no transaction in progress"
            :R "UserAbortTransactionBlock"}

           (-> @capture!
               (dissoc :L :F))))))


(deftest test-client-insert-result-no-returning
  (client/with-connection [conn CONFIG]

    (let [table
          (str (gensym "table"))

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (client/query conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2')" table)

          res
          (client/query conn query2)]

      (is (= 2 res)))))


(deftest test-client-select-fn-result
  (client/with-connection [conn CONFIG]

    (let [table
          (str (gensym "table"))

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (client/query conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2')" table)

          _
          (client/query conn query2)

          query3
          (format "select * from %s where id = 1" table)

          res
          (client/query conn query3 {:fn-result first})]

      (is (= {:id 1 :title "test1"} res)))))


(deftest test-client-delete-result
  (client/with-connection [conn CONFIG]

    (let [table
          (str (gensym "table"))

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (client/query conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2')" table)

          _
          (client/query conn query2)

          query3
          (format "delete from %s " table)

          res
          (client/query conn query3)]

      (is (= 2 res)))))


(deftest test-client-update-result
  (client/with-connection [conn CONFIG]

    (let [table
          (str (gensym "table"))

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (client/query conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2')" table)

          _
          (client/query conn query2)

          query3
          (format "update %s set title = 'aaa'" table)

          res
          (client/query conn query3)]

      (is (= 2 res)))))


(deftest test-client-mixed-result
  (client/with-connection [conn CONFIG]

    (let [table
          (str (gensym "table"))

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
          (client/query conn query {:fn-column str/upper-case})]

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
  (client/with-connection [conn CONFIG]

    (let [table
          (str (gensym "table"))

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (client/query conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2')" table)

          _
          (client/query conn query2)

          query3
          (format "truncate %s" table)

          res
          (client/query conn query3)]

      (is (nil? res)))))


(deftest test-client-select-multi

  (client/with-connection [conn CONFIG]

    (let [res
          (client/query conn "select 1 as foo; select 2 as bar")]

      (is (= [[{:foo 1}] [{:bar 2}]] res)))))


(deftest test-client-field-duplicates

  (client/with-connection [conn CONFIG]

    (let [res
          (client/query conn "select 1 as id, 2 as id")]

      (is (= [{:id_0 1 :id_1 2}] res)))))


(deftest test-client-as-vectors

  (client/with-connection [conn CONFIG]

    (let [res
          (client/query conn "select 1 as id, 2 as id" {:as-vectors? true})]

      (is (= [[1 2]] res)))))


(deftest test-client-as-java-maps

  (client/with-connection [conn CONFIG]

    (let [res
          (client/query conn "select 1 as id, 2 as id" {:as-java-maps? true})]

      (is (instance? java.util.HashMap (first res)))
      (is (= [{:id_0 1 :id_1 2}] res)))))
