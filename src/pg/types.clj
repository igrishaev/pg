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
   [pg.error :as e]
   [pg.codec :as codec]

   [clojure.java.io :as io]
   [clojure.xml :as xml]
   [clojure.string :as str]))


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
  (fn [value field enc]
    (:type-id field)))


(defmethod mm-parse-column-text :default
  [value field enc]
  (codec/bytes->str value enc))


(defmulti mm-parse-column-binary
  (fn [value field enc]
    (:type-id field)))


(defmethod mm-parse-column-binary :default
  [value field enc]
  value)


(defn parse-column-text
  [^bytes value
   {:as field :keys [type-id]}
   enc]

  (case (int type-id)

    16 ;; oid/BOOL
    (case (codec/bytes->str value enc)
      "t" true
      "f" false
      (e/error! "Wrong bool"
                {:value value
                 :field field
                 :in ::here}))

    (21 23) ;; oid/INT2 oid/INT4
    (-> value (codec/bytes->str enc) parseInt)

    20 ;; oid/INT8
    (-> value (codec/bytes->str enc) parseLong)

    700 ;; oid/FLOAT4
    (-> value (codec/bytes->str enc) parseFloat)

    701 ;; oid/FLOAT8
    (-> value (codec/bytes->str enc) parseDouble)

    2950 ;; oid/UUID
    (-> value (codec/bytes->str enc) parseUUID)

    142 ;; oid/XML
    (xml/parse (io/input-stream value :encoding enc))

    1700 ;; oid/NUMERIC
    (-> value (codec/bytes->str enc) bigdec)

    1184 ;; oid/TIMESTAMPTZ
    (-> value (codec/bytes->str enc) parse-ts-isoz)

    ;; 1082 | date
    ;; 1083 | time
    ;; 1114 | timestamp
    ;; 1186 | interval
    ;; 1266 | timetz

    ;; else
    (mm-parse-column-text value field enc)))


(defn parse-column-binary
  [^bytes value
   {:as field :keys [type-id]}
   enc]

  (case type-id

    ;; else
    (mm-parse-column-binary value field enc)))


(defn parse-column
  [^bytes value
   {:as field :keys [format type-id]}
   enc]

  (when (some? value)

    (case (int type-id)

      17 ;; oid/BYTEA
      value

      (25 1043) ;; oid/TEXT oid/VARCHAR
      (codec/bytes->str value enc)

      18 ;; oid/CHAR
      (char (aget value 0))

      ;; else
      (case (int format)

        0 ;; const/FORMAT_TEXT
        (parse-column-text value field enc)

        1 ;; const/FORMAT_BINARY
        (parse-column-binary value field enc)

        ;; else
        (e/error! "Wrong field format"
                  {:in ::here
                   :format format
                   :field field})))))
