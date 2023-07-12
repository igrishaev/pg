(ns pg.encode.txt-test
  (:import
   java.util.Date
   java.math.BigDecimal
   java.math.BigInteger)
  (:require
   [clojure.string :as str]
   [pg.oid :as oid]
   [pg.encode.txt :refer [encode]]
   [clojure.test :refer [deftest is testing]]))


(deftest test-encode

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
    (is (= "1985-12-31 20:59:59.0+00" res)))

  ;; TODO
  ;; Instant
  ;; LocalTime
  ;; OffsetTime
  ;; LocalDate
  ;; ZonedDateTime
  ;; OffsetDateTime
  ;; sql.Timestamp

  )
