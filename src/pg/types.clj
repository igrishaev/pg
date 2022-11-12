(ns pg.types
  "
  https://github.com/pgjdbc/pgjdbc/blob/0b097fd4a8e9990a9b86173d58633cd88d263b0b/pgjdbc/src/main/java/org/postgresql/core/Oid.java
  https://docs.oracle.com/javase/9/docs/api/java/time/format/DateTimeFormatter.html
  "
  (:import
   java.util.UUID
   java.time.ZoneId
   java.time.format.DateTimeFormatter
   java.time.Instant)
  (:require
   [clojure.java.io :as io]
   [clojure.xml :as xml]
   [clojure.string :as str]
   [pg.codec :as codec]))


(def ^DateTimeFormatter
  dtf-ts-isoz
  (-> "yyyy-MM-dd HH:mm:ss.nx"
      (DateTimeFormatter/ofPattern)
      (.withZone (ZoneId/of "UTC"))))


(defn parse-ts-isoz [string]
  (->> string
       (.parse dtf-ts-isoz)
       (Instant/from)))


(defn parseInt [x]
  (Integer/parseInt x))


(defn parseLong [x]
  (Long/parseLong x))


(defn parseFloat [x]
  (Float/parseFloat x))


(defn parseDouble [x]
  (Double/parseDouble x))


(defn parseUUID [x]
  (UUID/fromString x))


(defn parse-int-vec [x]
  (mapv parseInt
        (-> x
            (subs 1 (dec (count x)))
            (str/split #","))))


(defmulti parse-column-mm
  (fn [_value col-meta]
    (:type-id col-meta)))


(defmethod parse-column-mm :default
  [value _col-meta]
  (codec/bytes->str value))


(defn parse-column
  [^bytes value {:as column-meta
                 :keys [type-id]}]

  (when (some? value)

    (case (int type-id)

      17 ;; bytea
      value

      16 ;; bool
      (case (codec/bytes->str value)
        "t" true
        "f" false)

      ;; (114 3802) json, jsonb

      25 ;; text
      (-> value codec/bytes->str)

      18 ;; char
      (char (aget value 0))

      1043 ;; varchar
      (-> value codec/bytes->str)

      (2 23) ;; int2 int4
      (-> value codec/bytes->str parseInt)

      20 ;; int8
      (-> value codec/bytes->str parseLong)

      (22 1006) ;; int2vector _int2vector
      (-> value codec/bytes->str parse-int-vec)

      700 ;; float4
      (-> value codec/bytes->str parseFloat)

      701 ;; float8
      (-> value codec/bytes->str parseDouble)

      2950 ;; uuid
      (-> value codec/bytes->str parseUUID)

      142 ;; xml
      (xml/parse (io/input-stream value))

      1700 ;; numeric
      (-> value codec/bytes->str bigdec)

      1184 ;; timestamptz
      (-> value codec/bytes->str parse-ts-isoz)

      ;; 1082 | date
      ;; 1083 | time
      ;; 1114 | timestamp
      ;; 1186 | interval
      ;; 1266 | timetz

      ;; else
      (parse-column-mm value column-meta)

      ;; TODO

      ;; 600 | point
      ;; 601 | lseg
      ;; 602 | path
      ;; 603 | box
      ;; 604 | polygon
      ;; 628 | line

      ;; 650 | cidr

      ;; 705 | unknown
      ;; 718 | circle

      ;; 790 | money

      ;; 774 | macaddr8
      ;; 829 | macaddr
      ;; 869 | inet

      ;; 1042 | bpchar


      ;; 1560 | bit
      ;; 1562 | varbit

      ;; 2249 | record
      ;; 2275 | cstring
      ;; 2276 | any
      ;; 2277 | anyarray
      ;; 2278 | void

      ;; 3614 | tsvector
      ;; 3615 | tsquery

      ;; 4072 | jsonpath


      )

    ))
