(ns pg.client.client-test
  (:require
   [pg.client :as client]
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

    (is (= [{"foo" 1 "bar" "hello"}]
           result))))


(deftest test-client-reuse-conn

  (client/with-connection [conn CONFIG]

    (let [res1
          (client/query conn "select 1 as foo")
          res2
          (client/query conn "select 'hello' as bar")]

      (is (= [{"foo" 1}] res1))
      (is (= [{"bar" "hello"}] res2)))))


(deftest test-client-select-multi

  (client/with-connection [conn CONFIG]

    (let [res
          (client/query conn "select 1 as foo; select 2 as bar")]

      (is (= [[{"foo" 1}] [{"bar" 2}]] res)))))


(deftest test-client-field-duplicates

  (client/with-connection [conn CONFIG]

    (let [res
          (client/query conn "select 1 as id, 2 as id")]

      (is (= 1 res)))))


;; insert
;; insert + returning
;; update
;; delete
;; truncate
;; multi
