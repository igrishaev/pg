(ns pg.json-test
  (:require
   pg.json
   [pg.oid :as oid]
   [pg.encode.bin :as encode.bin]
   [pg.encode.txt :as encode.txt]
   [pg.decode.bin :as decode.bin]
   [pg.decode.txt :as decode.txt]
   [clojure.test :refer [deftest is]]))


(deftest test-json-txt

  (let [data
        {:foo [1 2 3]}

        encoded
        (encode.txt/encode data oid/json)

        decoded
        (decode.txt/decode encoded oid/jsonb)]

    (is (string? encoded))
    (is (= data decoded))))


(deftest test-json-txt-string

  (let [init
        "[1, 2, 3]"

        encoded
        (encode.txt/encode init oid/json)]

    (is (= init encoded))))


(deftest test-json-bin

  (let [data
        {:foo [1 2 3]}

        encoded
        (encode.bin/encode data oid/json)

        decoded
        (decode.bin/decode encoded oid/jsonb)]

    (is (bytes? encoded))
    (is (= data decoded))))


(deftest test-json-bin-string

  (let [data
        "[1, 2, 3]"

        encoded
        (encode.bin/encode data oid/json)]

    (is (bytes? encoded))
    (is (= data (new String encoded "UTF-8")))))
