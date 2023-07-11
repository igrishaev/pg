(ns pg.decode.txt-test
  (:import
   java.time.ZonedDateTime
   java.util.UUID
   java.math.BigDecimal)
  (:require
   [pg.oid :as oid]
   [pg.decode.txt :refer [decode]]
   [clojure.test :refer [deftest is]]))


(deftest test-numbers

  (let [res (decode "1" oid/int2)]
    (is (instance? Short res))
    (is (= 1 res)))

  (let [res (decode "1" oid/int4)]
    (is (instance? Integer res))
    (is (= 1 res)))

  (let [res (decode "1" oid/int8)]
    (is (instance? Long res))
    (is (= 1 res)))

  (let [res (decode "1.0" oid/float4)]
    (is (instance? Float res))
    (is (= 1.0 res)))

  (let [res (decode "1" oid/float8)]
    (is (instance? Double res))
    (is (= 1.0 res)))

  (let [res (decode "457452345254734563456456456456" oid/numeric)]
    (is (instance? BigDecimal res))
    (is (= 457452345254734563456456456456M res))))


(deftest test-strings

  (let [res (decode "test" oid/text)]
    (is (instance? String res))
    (is (= "test" res)))

  (let [res (decode "test" oid/varchar)]
    (is (instance? String res))
    (is (= "test" res)))

  (let [res (decode "t" oid/char)]
    (is (instance? Character res))
    (is (= \t res))))


(deftest test-bool

  (let [res (decode "f" oid/bool)]
    (is (false? res)))

  (let [res (decode "t" oid/bool)]
    (is (true? res)))

  (try
    (decode "x" oid/bool)
    (is false)
    (catch Exception e
      (is (= "cannot parse bool: x"
             (ex-message e)))
      (is (= {:string "x"
              :oid 16
              :opt nil}
             (ex-data e))))))


(deftest test-uuid
  (let [res (decode "6e6388d3-3930-47f5-bfe4-88d9588e0edb" oid/uuid)]
    (is (= #uuid "6e6388d3-3930-47f5-bfe4-88d9588e0edb"
           res))))


(deftest test-date-time

  (let [string
        "2023-07-10 22:25:22.046553+03"

        res
        (decode string oid/timestamptz)]

    (is (instance? ZonedDateTime res))
    (is (= "2023-07-10T19:25:22.000046553Z[UTC]"
           (str res)))))
