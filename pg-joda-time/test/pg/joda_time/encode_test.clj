(ns pg.joda-time.encode-test
  (:import
   org.joda.time.DateTime)
  (:require
   pg.joda-time
   [pg.oid :as oid]
   [clj-time.core :as t]
   [pg.encode.bin :as encode.bin]
   [pg.encode.txt :as encode.txt]
   [pg.decode.bin :as decode.bin]
   [pg.decode.txt :as decode.txt]
   [clojure.test :refer [deftest is testing]]))


(deftest test-encode-txt

  (let [x1 (t/now)
        string (encode.txt/encode x1 oid/timestamptz)
        x2 (decode.txt/decode string oid/timestamptz)
        ]
    (is (= x1 x2))
    )



  )
