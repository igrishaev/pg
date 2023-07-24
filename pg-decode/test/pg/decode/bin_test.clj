(ns pg.decode.bin-test
  (:import
   java.time.Instant
   java.time.OffsetTime
   java.time.LocalTime
   java.time.LocalDate
   java.time.LocalDateTime
   java.util.UUID
   java.math.BigDecimal)
  (:require
   [pg.oid :as oid]
   [pg.decode.bin :as bin]
   [clojure.test :refer [deftest is testing]]))


(deftest test-uuid

  (let [buf
        (byte-array [-69 -39 -49 124 78 1 78 115 -103 -87 -115 94 88 11 -64 20])

        uuid
        (bin/decode buf oid/uuid)]

    (is (= #uuid "bbd9cf7c-4e01-4e73-99a9-8d5e580bc014" uuid))))
