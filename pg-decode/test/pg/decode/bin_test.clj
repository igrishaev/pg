(ns pg.decode.bin-test
  (:import
   java.time.OffsetTime
   java.time.OffsetDateTime
   java.time.LocalTime
   java.time.LocalDate
   java.time.LocalDateTime
   java.util.UUID
   java.math.BigDecimal)
  (:require
   [pg.oid :as oid]
   [pg.decode.bin :as bin]
   [clojure.test :refer [deftest is testing]]))


;; TODO: complete the tests

(deftest test-uuid

  (let [buf
        (byte-array [-69 -39 -49 124 78 1 78 115 -103 -87 -115 94 88 11 -64 20])

        uuid
        (bin/decode buf oid/uuid)]

    (is (= #uuid "bbd9cf7c-4e01-4e73-99a9-8d5e580bc014" uuid))))


(deftest test-datetime

  (testing "timestamptz"

    (let [buf ;; 2022-01-01 12:01:59.123456789+03
          (byte-array [0 2 119 -128 79 11 -14 1])

          res
          (bin/decode buf oid/timestamptz)]

      (is (= "2022-01-01T09:01:59.123457Z" (str res)))
      (is (instance? OffsetDateTime res))))

  (testing "timestamp"

    (let [buf ;; 2022-01-01 12:01:59.123456789
          (byte-array [0 2 119 -126 -46 -58 -34 1])

          res
          (bin/decode buf oid/timestamp)]

      (is (= "2022-01-01T12:01:59.123457" (str res)))
      (is (instance? LocalDateTime res))))

  (testing "date"

    )

  (testing "timetz"

    )

  (testing "time"

    )





  )
