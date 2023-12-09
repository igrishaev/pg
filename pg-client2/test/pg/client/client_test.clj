(ns pg.client.client-test
  (:import
   com.github.igrishaev.PGError)
  (:require
   [pg.client :as pg]
   [clojure.test :refer [deftest is use-fixtures testing]]))


(def ^:dynamic *CONFIG*
  {:host "127.0.0.1"
   :port 10130
   :user "test"
   :password "test"
   :database "test"})


(deftest test-client-tx-status

  (pg/with-connection [conn *CONFIG*]

    (is (= :I (pg/status conn)))

    (is (pg/idle? conn))

    (pg/query conn "select 1")

    (is (= :I (pg/status conn)))

    (pg/begin conn)

    (is (= :T (pg/status conn)))

    (is (pg/in-transaction? conn))

    (try
      (pg/query conn "selekt 1")
      (is false)
      (catch PGError _
        nil))

    (is (= :E (pg/status conn)))

    (is (pg/tx-error? conn))

    (pg/rollback conn)

    (is (= :I (pg/status conn)))))
