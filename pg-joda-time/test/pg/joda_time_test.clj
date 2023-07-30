(ns pg.joda-time-test
  (:import
   org.joda.time.DateTime
   org.joda.time.DateTimeZone)
  (:require
   pg.joda-time
   [pg.oid :as oid]
   [clj-time.core :as t]
   [pg.encode.bin :as encode.bin]
   [pg.encode.txt :as encode.txt]
   [pg.decode.bin :as decode.bin]
   [pg.decode.txt :as decode.txt]
   [clojure.test :refer [deftest is testing]]))


(deftest test-txt

  ;; timestamptz

  (let [v1 (new DateTime "2023-07-30T01:34:18+03:00")
        string (encode.txt/encode v1 oid/timestamptz)
        ^DateTime v2 (decode.txt/decode string oid/timestamptz)]

    (is (instance? DateTime v2))
    (is (= "2023-07-29 22:34:18.000000+00" string))

    (is (= (.withZone v1 DateTimeZone/UTC)
           (.withZone v2 DateTimeZone/UTC))))

  ;; timestamp

  (let [v1 (new DateTime "2023-07-30T16:34:18.894Z")
        string (encode.txt/encode v1 oid/timestamp)
        ^DateTime v2 (decode.txt/decode string oid/timestamp)]

    (is (= "2023-07-30 16:34:18.894000" string))
    (is (instance? DateTime v2))

    (is (= (.withZone v1 DateTimeZone/UTC)
           (.withZone v2 DateTimeZone/UTC))))

  ;; date

  (let [v1 (new DateTime "2023-07-30T01:34:18+03:00")
        string (encode.txt/encode v1 oid/date)
        ^DateTime v2 (decode.txt/decode string oid/date)]

    (is (= "2023-07-29" string))
    (is (instance? DateTime v2))

    (is (= (new DateTime "2023-07-29T03:00:00.000+03:00")
           v2))))


(deftest test-bin

  ;; timestamptz

  (let [v1 (new DateTime "2023-07-30T01:34:18+03:00")
        buf (encode.bin/encode v1 oid/timestamptz)
        ^DateTime v2 (decode.bin/decode buf oid/timestamptz)]

    (is (= [0 2 -92 -90 -111 5 110 -128] (vec buf)))
    (is (instance? DateTime v2))

    (is (= (.withZone v1 DateTimeZone/UTC)
           (.withZone v2 DateTimeZone/UTC))))

  ;; timestamp

  (let [v1 (new DateTime "2023-07-30T16:34:18.894Z")
        buf (encode.bin/encode v1 oid/timestamp)
        ^DateTime v2 (decode.bin/decode buf oid/timestamp)]

    (is (= [0 2 -92 -75 -89 116 -102 -80] (vec buf)))
    (is (instance? DateTime v2))

    (is (= (.withZone v1 DateTimeZone/UTC)
           (.withZone v2 DateTimeZone/UTC))))

  ;; date

  (let [v1 (new DateTime "2023-07-30T01:34:18+03:00")
        buf (encode.bin/encode v1 oid/date)
        ^DateTime v2 (decode.bin/decode buf oid/date)]

    (is (= [0 0 33 -94] (vec buf)))
    (is (instance? DateTime v2))

    (is (= (new DateTime "2023-07-29T03:00:00.000+03:00")
           v2))))
