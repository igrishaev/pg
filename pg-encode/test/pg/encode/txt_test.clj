(ns pg.encode.txt-test
  (:import
   java.util.Date
   java.time.Instant
   java.time.OffsetDateTime
   java.time.LocalDateTime
   java.math.BigDecimal
   java.math.BigInteger)
  (:require
   [clojure.string :as str]
   [pg.oid :as oid]
   [pg.encode.txt :refer [encode]]
   [clojure.test :refer [deftest is testing]]))


(deftest test-encode-basic

  (is (= "1" (encode 1)))

  (is (= "1" (encode (int 1))))

  (is (= "1" (encode (short 1))))

  (is (= "1.1" (encode 1.1)))

  (is (= "1.1" (encode (float 1.1))))

  (is (= "1.0000000000001" (encode 1.0000000000001)))

  (is (= "t" (encode true)))

  (is (= "f" (encode false)))

  (try
    (= "f" (encode nil))
    (is false)
    (catch Exception e
      (is (= "Cannot text-encode a value"
             (ex-message e)))))

  (let [uuid (random-uuid)]
    (is (= (str uuid) (encode uuid))))

  (is (= "foo/bar" (encode 'foo/bar)))

  (is (= "1.0E+54" (encode (bigdec 999999999999999999999999999999999999999999999999999999.999999))))

  (is (= "?" (encode \?)))

  (let [res (encode (new BigDecimal 999.999))]
    (is (str/starts-with? res "999.999")))

  (let [res (encode (new BigInteger "999"))]
    (is (= "999" res)))

  (let [res (encode (bigint 999.888))]
    (is (= "999" res)))

  (let [date (new Date 85 11 31 23 59 59)
        res (encode date)]
    (is (= "1985-12-31 20:59:59.000000+00" res))))


(deftest test-datetime

  (testing "OffsetDateTime default"
    (let [val (OffsetDateTime/parse "2023-07-25T01:00:00.123456+03")
          res (encode val)]
      (is (= "2023-07-24 22:00:00.123456+00" res))))

  (testing "OffsetDateTime timestamptz"
    (let [val (OffsetDateTime/parse "2023-07-25T01:00:00.123456+03")
          res (encode val oid/timestamptz)]
      (is (= "2023-07-24 22:00:00.123456+00" res))))

  (testing "OffsetDateTime timestamp"
    (let [val (OffsetDateTime/parse "2023-07-25T01:00:00.123456+03")
          res (encode val oid/timestamp)]
      (is (= "2023-07-24 22:00:00.123456" res))))

  (testing "OffsetDateTime date"
    (let [val (OffsetDateTime/parse "2023-07-25T01:00:00.123456+03")
          res (encode val oid/date)]
      (is (= "2023-07-24" res))))

  (testing "LocalDateTime default"
    (let [val (LocalDateTime/parse "2023-07-25T01:00:00.123456")
          res (encode val)]
      (is (= "2023-07-25 01:00:00.123456" res))))

  (testing "LocalDateTime timestamp"
    (let [val (LocalDateTime/parse "2023-07-25T01:00:00.123456")
          res (encode val oid/timestamp)]
      (is (= "2023-07-25 01:00:00.123456" res))))

  (testing "LocalDateTime timestamptz"
    (let [val (LocalDateTime/parse "2023-07-25T01:00:00.123456")
          res (encode val oid/timestamptz)]
      (is (= "2023-07-25 01:00:00.123456+00" res))))

  (testing "LocalDateTime date"
    (let [val (LocalDateTime/parse "2023-07-25T01:00:00.123456")
          res (encode val oid/date)]
      (is (= "2023-07-25" res))))





  #_
  (let [res (encode (new Date))]
    (is (= 1 res))
    )

  )

;; ZonedDateTime
;; LocalTime
;; OffsetTime
;; LocalDate
;; Instant
;; Date
