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


(deftest test-client-fn-column

  (let [result
        (client/with-connection [conn CONFIG]
          (client/query conn "select 1 as foo" {:fn-column str/upper-case}))]

    (is (= [{"FOO" 1}] result))))


(deftest test-client-reuse-conn

  (client/with-connection [conn CONFIG]

    (let [res1
          (client/query conn "select 1 as foo")
          res2
          (client/query conn "select 'hello' as bar")]

      (is (= [{:foo 1}] res1))
      (is (= [{:bar "hello"}] res2)))))


(deftest test-client-create-table
  (client/with-connection [conn CONFIG]

    (let [table
          (str (gensym "table"))

          query
          (format "create temp table %s (id serial, title text)" table)

          res
          (client/query conn query)]

      (is (nil? res)))))


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


#_
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

      (is (= 1
             res)))))


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


;; insert
;; insert + returning
;; update
;; delete
;; truncate
;; multi
