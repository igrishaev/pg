(ns pg.decode-txt-test
  (:import
   com.github.igrishaev.PGError
   com.github.igrishaev.codec.DecoderTxt
   java.time.OffsetTime
   java.time.LocalTime
   java.time.LocalDate
   java.time.OffsetDateTime
   java.time.LocalDateTime
   java.util.UUID
   java.math.BigDecimal)
  (:require
   [pg.oid :as oid]
   [clojure.test :refer [deftest is testing]]))


(defn decode [obj oid]
  (DecoderTxt/decode obj oid))


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


(deftest test-oid-name

  (let [res (decode "test" oid/name)]
    (is (instance? String res))
    (is (= "test" res)))

  (let [res (decode "42" oid/oid)]
    (is (instance? Integer res))
    (is (= 42 res))))


(deftest test-bool

  (let [res (decode "f" oid/bool)]
    (is (false? res)))

  (let [res (decode "t" oid/bool)]
    (is (true? res)))

  (try
    (decode "x" oid/bool)
    (is false)
    (catch PGError e
      (is (= "wrong boolean value: x"
             (ex-message e))))))


(deftest test-uuid
  (let [res (decode "6e6388d3-3930-47f5-bfe4-88d9588e0edb" oid/uuid)]
    (is (= #uuid "6e6388d3-3930-47f5-bfe4-88d9588e0edb"
           res))))


(deftest test-date-time

  (testing "timestamptz"

    (let [string
          "2023-07-10 22:25:22.046553+03"

          res
          (decode string oid/timestamptz)]

      (is (instance? OffsetDateTime res))
      (is (= "2023-07-10T22:25:22.046553+03:00"
             (str res))))

    (let [string
          "2023-07-10 22:25:22.046553"

          res
          (decode string oid/timestamp)]

      (is (instance? LocalDateTime res))
      (is (= "2023-07-10T22:25:22.046553"
             (str res))))

    (let [string
          "2022-07-03 00:00:00+03"

          res
          (decode string oid/timestamptz)]

      (is (instance? OffsetDateTime res))
      (is (= "2022-07-03T00:00+03:00"
             (str res)))))

  (testing "date"

    (let [string
          "2022-07-03"

          res
          (decode string oid/date)]

      (is (instance? LocalDate res))
      (is (= "2022-07-03"
             (str res)))))

  (testing "timetz"

    (let [string
          "10:29:39.853741+03"

          res
          (decode string oid/timetz)]

      (is (instance? OffsetTime res))
      (is (= "10:29:39.853741+03:00"
             (str res))))

    (let [string
          "10:29:39+03"

          res
          (decode string oid/timetz)]

      (is (instance? OffsetTime res))
      (is (= "10:29:39+03:00"
             (str res)))))

  (testing "time"

    (let [string
          "10:29:39"

          res
          (decode string oid/time)]

      (is (instance? LocalTime res))
      (is (= "10:29:39"
             (str res))))

    (let [string
          "10:29:39.1234"

          res
          (decode string oid/time)]

      (is (instance? LocalTime res))
      (is (= "10:29:39.123400"
             (str res))))))


(deftest test-decode-array

  (testing "trivial ints"
    (let [string
          "{1,2,3}"
          res1
          (decode string oid/_int2)
          res2
          (decode string oid/_int4)
          res3
          (decode string oid/_int8)]
      (is (= [1 2 3] res1 res2 res3))))

  (testing "empty"
    (let [string
          "{}"
          res1
          (decode string oid/_int2)
          res2
          (decode string oid/_int4)
          res3
          (decode string oid/_int8)]
      (is (= [] res1 res2 res3))))

  (testing "text"
    (let [string
          "{foo,bar,baz}"
          res
          (decode string oid/_text)]
      (is (= ["foo" "bar" "baz"] res))))

  (testing "empty lines"
    (let [string
          "{\"\",\"\",\"\"}"
          res
          (decode string oid/_text)]
      (is (= ["" "" ""] res))))

  (testing "null literals mixed"
    (let [string
          "{null,\"null\",NULL,\"NULL\",nullable,NULLABLE}"
          res
          (decode string oid/_text)]
      (is (= [nil "null" nil "NULL" "nullable" "NULLABLE"] res))))

  (testing "null literals case"
    (let [string
          "{null,NULL,Null}"
          res
          (decode string oid/_text)]
      (is (= [nil nil nil] res))))

  (testing "quotes, slashes, spaces"
    (let [string
          "{null,\"foo\\\"bar\",\"C:\\\\windows\"}"
          res
          (decode string oid/_text)]
      (is (= [nil "foo\"bar" "C:\\windows"] res))))

  (testing "multi-dim"
    (let [string
          "{{{a,b},{c,d}},{{e,f},{g,h}}}"
          res
          (decode string oid/_text)]
      (is (= [[["a" "b"] ["c" "d"]]
              [["e" "f"] ["g" "h"]]] res))))

  (testing "multi-dim"
    (let [string
          "{{{a,b},{c,d}},{{e,f},{g,h}}}"
          res
          (decode string oid/_text)]
      (is (= [[["a" "b"] ["c" "d"]]
              [["e" "f"] ["g" "h"]]] res))))

  (testing "multi-dim ts"
    (let [string
          "{{{\"2023-09-13 09:13:47.708253+00\",\"2023-09-13 09:13:47.708253+01\"},{\"2023-09-13 09:13:47.708253+02\",\"2023-09-13 09:13:47.708253+03\"}},{{\"2023-09-13 09:13:47.708253+04\",\"2023-09-13 09:13:47.708253+05\"},{\"2023-09-13 09:13:47.708253+06\",\"2023-09-13 09:13:47.708253+07\"}}}"
          res
          (decode string oid/_timestamptz)]

      (is (= [[[(OffsetDateTime/parse "2023-09-13T09:13:47.708253+00:00")
                (OffsetDateTime/parse "2023-09-13T09:13:47.708253+01:00")]
               [(OffsetDateTime/parse "2023-09-13T09:13:47.708253+02:00")
                (OffsetDateTime/parse "2023-09-13T09:13:47.708253+03:00")]]
              [[(OffsetDateTime/parse "2023-09-13T09:13:47.708253+04:00")
                (OffsetDateTime/parse "2023-09-13T09:13:47.708253+05:00")]
               [(OffsetDateTime/parse "2023-09-13T09:13:47.708253+06:00")
                (OffsetDateTime/parse "2023-09-13T09:13:47.708253+07:00")]]]
             res)))))
