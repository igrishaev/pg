(ns pg.encode-txt-test
  (:import
   com.github.igrishaev.enums.OID
   com.github.igrishaev.codec.EncoderTxt
   java.util.Date
   java.time.Instant
   java.time.LocalDate
   java.time.LocalTime
   java.time.OffsetTime
   java.time.ZonedDateTime
   java.time.OffsetDateTime
   java.time.LocalDateTime
   java.math.BigDecimal
   java.math.BigInteger)
  (:require
   [clojure.string :as str]
   [pg.oid :as oid]
   [clojure.test :refer [deftest is testing]]))


(defn encode
  ([obj]
   (EncoderTxt/encode obj))
  ([obj ^OID oid]
   (EncoderTxt/encode obj oid)))


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
    (is (= "999" res))))


(deftest test-oid-name

  (let [val 42
        res (encode val oid/oid)]
    (is (= "42" res)))

  (let [val "hello"
        res (encode val oid/name)]
    (is (= "hello" res))))


(deftest test-datetime

  (testing "OffsetDateTime default"
    (let [val (OffsetDateTime/parse "2023-07-25T01:00:00.123456+03")
          res (encode val nil)]
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
          res (encode val nil)]
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

  (testing "ZonedDateTime default"
    (let [val (ZonedDateTime/parse "2023-07-25T01:00:00.123456+03")
          res (encode val nil)]
      (is (= "2023-07-24 22:00:00.123456+00" res))))

  (testing "ZonedDateTime timestsamptz"
    (let [val (ZonedDateTime/parse "2023-07-25T01:00:00.123456+03")
          res (encode val oid/timestamptz)]
      (is (= "2023-07-24 22:00:00.123456+00" res))))

  (testing "ZonedDateTime timestsamp"
    (let [val (ZonedDateTime/parse "2023-07-25T01:00:00.123456+03")
          res (encode val oid/timestamp)]
      (is (= "2023-07-24 22:00:00.123456" res))))

  (testing "ZonedDateTime date"
    (let [val (ZonedDateTime/parse "2023-07-25T01:00:00.123456+03")
          res (encode val oid/date)]
      (is (= "2023-07-24" res))))

  (testing "LocalTime default"
    (let [val (LocalTime/parse "01:00:00.123456")
          res (encode val nil)]
      (is (= "01:00:00.123456" res))))

  (testing "LocalTime time"
    (let [val (LocalTime/parse "01:00:00.123456")
          res (encode val oid/time)]
      (is (= "01:00:00.123456" res))))

  (testing "LocalTime timetz"
    (let [val (LocalTime/parse "01:00:00.123456")
          res (encode val oid/timetz)]
      (is (= "01:00:00.123456+00" res))))

  (testing "OffsetTime default"
    (let [val (OffsetTime/parse "01:00:00.123456+03:00")
          res (encode val nil)]
      (is (= "01:00:00.123456+03" res))))

  (testing "OffsetTime timetz"
    (let [val (OffsetTime/parse "01:00:00.123456+03:00")
          res (encode val oid/timetz)]
      (is (= "01:00:00.123456+03" res))))

  (testing "OffsetTime time"
    (let [val (OffsetTime/parse "01:00:00.123456+03:00")
          res (encode val oid/time)]
      (is (= "22:00:00.123456" res))))

  (testing "LocalDate default"
    (let [val (LocalDate/parse "2022-01-01")
          res (encode val nil)]
      (is (= "2022-01-01" res))))

  (testing "LocalDate date"
    (let [val (LocalDate/parse "2022-01-01")
          res (encode val oid/date)]
      (is (= "2022-01-01" res))))

  (testing "LocalDate timestamp"
    (let [val (LocalDate/parse "2022-01-01")
          res (encode val oid/timestamp)]
      (is (= "2022-01-01 00:00:00.000000" res))))

  (testing "LocalDate timestamptz"
    (let [val (LocalDate/parse "2022-01-01")
          res (encode val oid/timestamptz)]
      (is (= "2022-01-01 00:00:00.000000+00" res))))

  (testing "Instant default"
    (let [val (Instant/parse "2023-07-25T01:00:00.123456Z")
          res (encode val nil)]
      (is (= "2023-07-25 01:00:00.123456+00" res))))

  (testing "Instant timestamptz"
    (let [val (Instant/parse "2023-07-25T01:00:00.123456Z")
          res (encode val oid/timestamptz)]
      (is (= "2023-07-25 01:00:00.123456+00" res))))

  (testing "Instant timestamp"
    (let [val (Instant/parse "2023-07-25T01:00:00.123456Z")
          res (encode val oid/timestamp)]
      (is (= "2023-07-25 01:00:00.123456" res))))

  (testing "Instant date"
    (let [val (Instant/parse "2023-07-25T01:00:00.123456Z")
          res (encode val oid/date)]
      (is (= "2023-07-25" res))))

  (testing "Date default"
    (let [val (-> "2023-07-25T01:00:00.123456Z"
                  (Instant/parse)
                  (.toEpochMilli)
                  (Date.))
          res (encode val nil)]
      (is (= "2023-07-25 01:00:00.123000+00" res))))

  (testing "Date timestamptz"
    (let [val (-> "2023-07-25T01:00:00.123456Z"
                  (Instant/parse)
                  (.toEpochMilli)
                  (Date.))
          res (encode val oid/timestamptz)]
      (is (= "2023-07-25 01:00:00.123000+00" res))))

  (testing "Date timestamp"
    (let [val (-> "2023-07-25T01:00:00.123456Z"
                  (Instant/parse)
                  (.toEpochMilli)
                  (Date.))
          res (encode val oid/timestamp)]
      (is (= "2023-07-25 01:00:00.123000" res))))

  (testing "Date date"
    (let [val (-> "2023-07-25T01:00:00.123456Z"
                  (Instant/parse)
                  (.toEpochMilli)
                  (Date.))
          res (encode val oid/date)]
      (is (= "2023-07-25" res)))))
