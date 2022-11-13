(ns pg.types
  "
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

   [pg.oid :as oid]
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


(defmulti mm-parse-column-text
  (fn [_value col-meta]
    (:type-id col-meta)))


(defmethod mm-parse-column-text :default
  [value _col-meta]
  (codec/bytes->str value))


(defmulti mm-parse-column-binary
  (fn [_value col-meta]
    (:type-id col-meta)))


(defmethod mm-parse-column-binary :default
  [value _col-meta]
  value)


(defn parse-column-text
  [^bytes value {:as column-meta
                 :keys [type-id]}]

  (case (int type-id)

    16 ;; oid/BOOL
    (case (codec/bytes->str value)
      "t" true
      "f" false)

    21 ;; oid/INT2
    (-> value codec/bytes->str parseInt)

    23 ;; oid/INT4
    (-> value codec/bytes->str parseInt)

    20 ;; oid/INT8
    (-> value codec/bytes->str parseLong)

    700 ;; oid/FLOAT4
    (-> value codec/bytes->str parseFloat)

    701 ;; oid/FLOAT8
    (-> value codec/bytes->str parseDouble)

    2950 ;; oid/UUID
    (-> value codec/bytes->str parseUUID)

    142 ;; oid/XML
    (xml/parse (io/input-stream value))

    1700 ;; oid/NUMERIC
    (-> value codec/bytes->str bigdec)

    1184 ;; oid/TIMESTAMPTZ
    (-> value codec/bytes->str parse-ts-isoz)

    ;; 1082 | date
    ;; 1083 | time
    ;; 1114 | timestamp
    ;; 1186 | interval
    ;; 1266 | timetz

    ;; else
    (mm-parse-column-text value column-meta)))


(defn parse-column-binary
  [^bytes value {:as column-meta
                 :keys [type-id]}]

  (case type-id

    ;; else
    (mm-parse-column-binary value column-meta)))


(defn parse-column
  [^bytes value {:as column-meta
                 :keys [format type-id]}]

  (when (some? value)

    (case (int type-id)

      oid.BYTEA
      value

      oid.TEXT
      (-> value codec/bytes->str)

      oid.VARCHAR
      (-> value codec/bytes->str)

      oid.CHAR
      (char (aget value 0))

      ;; else
      (case format

        const/FORMAT_TEXT
        (parse-column-text value column-meta)

        const/FORMAT_BINARY
        (parse-column-binary value column-meta)

        ;; else
        (throw (ex-info "Wrong field format"
                        {:column-meta column-meta}))))))
