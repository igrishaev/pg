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

        string
        (encode.txt/encode data oid/json)

        decoded
        (decode.txt/decode string oid/jsonb)]

    (is (string? string))
    (is (= data decoded))))


(deftest test-json-bin

  (let [data
        {:foo [1 2 3]}

        string
        (encode.bin/encode data oid/json)

        decoded
        (decode.bin/decode string oid/jsonb)]

    (is (bytes? string))
    (is (= data decoded))))
