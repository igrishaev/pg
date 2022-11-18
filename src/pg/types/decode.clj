(ns pg.types.decode
  "
  https://docs.oracle.com/javase/9/docs/api/java/time/format/DateTimeFormatter.html
  https://github.com/pgjdbc/pgjdbc/blob/master/pgjdbc/src/main/java/org/postgresql/jdbc/TimestampUtils.java
  "
  (:import
   java.util.UUID
   java.time.ZoneId
   java.time.format.DateTimeFormatter
   java.time.Instant)
  (:require
   [pg.oid :as oid]
   [pg.error :as e]
   [pg.codec :as codec]

   [clojure.java.io :as io]
   [clojure.xml :as xml]
   [clojure.string :as str]))


;;
;; Utils
;;

(defn parseUUID [x]
  (UUID/fromString x))


(defn parseInt [x]
  (Integer/parseInt x))


(defn parseLong [x]
  (Long/parseLong x))


(defn parseFloat [x]
  (Float/parseFloat x))


(defn parseDouble [x]
  (Double/parseDouble x))


(def ^DateTimeFormatter dtf-timestamp-z-iso
  (-> "yyyy-MM-dd HH:mm:ss.nx"
      (DateTimeFormatter/ofPattern)
      (.withZone (ZoneId/of "UTC"))))


(defn parseTimestamptZ [string]
  (->> string
       (.parse dtf-timestamp-z-iso)
       (Instant/from)))


;;
;; Text
;;

(defmulti decode-text
  (fn [bytes field enc]
    (:type-id field)))


(defmethod decode-text :default
  [bytes field enc]
  (codec/bytes->str bytes enc))


(defmethod decode-text oid/BYTEA
  [bytes field enc]
  bytes)


(defmethod decode-text oid/TEXT
  [bytes field enc]
  (codec/bytes->str bytes enc))


(defmethod decode-text oid/VARCHAR
  [bytes field enc]
  (codec/bytes->str bytes enc))


(defmethod decode-text oid/CHAR
  [^bytes bytes field enc]
  (char (aget bytes 0)))


(defmethod decode-text oid/BOOL
  [bytes field enc]
  (let [val
        (codec/bytes->str bytes enc)]
    (case val
      "t" true
      "f" false
      val)))


(defmethod decode-text oid/INT2
  [bytes field enc]
  (-> bytes (codec/bytes->str enc) parseInt))


(defmethod decode-text oid/INT4
  [bytes field enc]
  (-> bytes (codec/bytes->str enc) parseInt))


(defmethod decode-text oid/INT8
  [bytes field enc]
  (-> bytes (codec/bytes->str enc) parseLong))


(defmethod decode-text oid/FLOAT4
  [bytes field enc]
  (-> bytes (codec/bytes->str enc) parseFloat))


(defmethod decode-text oid/FLOAT8
  [bytes field enc]
  (-> bytes (codec/bytes->str enc) parseDouble))


(defmethod decode-text oid/UUID
  [bytes field enc]
  (-> bytes (codec/bytes->str enc) parseUUID))


(defmethod decode-text oid/XML
  [bytes field enc]
  (xml/parse (io/input-stream bytes :encoding enc)))


(defmethod decode-text oid/NUMERIC
  [bytes field enc]
  (-> bytes (codec/bytes->str enc) bigdec))


(defmethod decode-text oid/TIMESTAMPTZ
  [bytes field enc]
  (-> bytes (codec/bytes->str enc) parseTimestamptZ))

;; 1082 | date
;; 1083 | time
;; 1114 | timestamp
;; 1186 | interval
;; 1266 | timetz


;;
;; Binary
;;

(defmulti decode-binary
  (fn [bytes field enc]
    (:type-id field)))


(defmethod decode-binary :default
  [bytes field enc]
  bytes)


(defmethod decode-binary oid/FLOAT4
  [^bytes bytes field enc]
  (Float/intBitsToFloat (new BigInteger bytes)))


(defmethod decode-binary oid/TEXT
  [^bytes bytes field enc]
  (codec/bytes->str bytes enc))


(defmethod decode-binary oid/FLOAT8
  [^bytes bytes field enc]
  (Double/longBitsToDouble (new BigInteger bytes)))


(defmethod decode-binary oid/BOOL
  [^bytes bytes field enc]
  (case (aget bytes 0)
    0 false
    1 true))


(defmethod decode-binary oid/INT2
  [^bytes bytes field enc]
  (.shortValue (new BigInteger bytes)))


(defmethod decode-binary oid/INT4
  [^bytes bytes field enc]
  (.intValue (new BigInteger bytes)))


(defmethod decode-binary oid/INT8
  [^bytes bytes field enc]
  (.longValue (new BigInteger bytes)))


(defmethod decode-binary oid/NUMERIC
  [^bytes bytes field enc]
  ()
  (.longValue (new BigInteger bytes)))


;;
;; The main entry point
;;

(defn decode
  [bytes
   {:as field field-format :format}
   enc]
  (case (int field-format)
    0 (decode-text bytes field enc)
    1 (decode-binary bytes field enc)))
