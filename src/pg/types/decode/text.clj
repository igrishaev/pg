(ns pg.types.decode.text
  "
  https://github.com/pgjdbc/pgjdbc/blob/master/pgjdbc/src/main/java/org/postgresql/util/ByteConverter.java#L124
  https://docs.oracle.com/javase/9/docs/api/java/time/format/DateTimeFormatter.html
  https://github.com/pgjdbc/pgjdbc/blob/master/pgjdbc/src/main/java/org/postgresql/jdbc/TimestampUtils.java
  https://github.com/pgjdbc/pgjdbc/blob/master/pgjdbc/src/main/java/org/postgresql/util/PGbytea.java
  https://www.postgresql.org/message-id/attachment/13504/arrayAccess.txt
  https://postgrespro.ru/docs/postgresql/14/runtime-config-client
  "
  (:import
   java.io.ByteArrayOutputStream
   java.util.UUID
   java.time.ZoneId
   java.time.LocalDate
   java.time.LocalTime
   java.time.LocalDateTime
   java.time.format.DateTimeFormatter
   java.time.Instant)
  (:require
   [pg.const :as const]
   [pg.bb :as bb]
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


(def ^DateTimeFormatter dtf-date
  (-> "yyyy-MM-dd"
      (DateTimeFormatter/ofPattern)))


(defn parse-date [string]
  (->> string
       (.parse dtf-date)
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
  [^bytes buf field enc]

  (cond

    ;; starts with \x
    ;; bytea_output = 'hex'
    (and (= (get buf 0) 92)
         (= (get buf 1) 120))
    (let [len (-> buf
                  (alength)
                  (/ 2)
                  (dec))
          out
          (new ByteArrayOutputStream)]

      (loop [i 1]
        (if (> i len)
          (.toByteArray out)
          (let [b1 (aget buf (* i 2))
                b2 (aget buf (inc (* i 2)))
                hex (str (char b1) (char b2))
                b (unchecked-byte (Integer/parseInt hex 16))]
            (.write out b)
            (recur (inc i))))))

    ;; TODO: parse escaped string
    ;; bytea_output = 'escape'
    :else
    (e/error! "only hex bytea output supported at the moment")))


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


(defmethod decode-text oid/DATE
  [bytes field enc]
  (LocalDate/parse (codec/bytes->str bytes enc)))


(defmethod decode-text oid/TIME
  [bytes field enc]
  (LocalTime/parse (codec/bytes->str bytes enc)))


;; 1082 | date
;; 1083 | time
;; 1114 | timestamp
;; 1186 | interval
;; 1266 | timetz


;; (defmulti decode-binary
;;   (fn [bytes field enc]
;;     (:type-id field)))


;; (defmethod decode-binary :default
;;   [bytes field enc]
;;   bytes)


;; (defmethod decode-binary oid/FLOAT4
;;   [^bytes bytes field enc]
;;   (Float/intBitsToFloat (new BigInteger bytes)))


;; (defmethod decode-binary oid/TEXT
;;   [^bytes bytes field enc]
;;   (codec/bytes->str bytes enc))


;; (defmethod decode-binary oid/FLOAT8
;;   [^bytes bytes field enc]
;;   (Double/longBitsToDouble (new BigInteger bytes)))


;; (defmethod decode-binary oid/BOOL
;;   [^bytes bytes field enc]
;;   (case (aget bytes 0)
;;     0 false
;;     1 true))


;; ;; TODO: use bb?
;; (defmethod decode-binary oid/INT2
;;   [^bytes bytes field enc]
;;   (.shortValue (new BigInteger bytes)))


;; (defmethod decode-binary oid/INT4
;;   [^bytes bytes field enc]
;;   (.intValue (new BigInteger bytes)))


;; (defmethod decode-binary oid/INT8
;;   [^bytes bytes field enc]
;;   (.longValue (new BigInteger bytes)))


;; (defmethod decode-binary oid/NUMERIC
;;   [^bytes bytes field enc]
;;   (.longValue (new BigInteger bytes)))


;; ;;
;; ;; Binary arrays
;; ;;

;; (defn get-dims [limits n]
;;   (let [len (count limits)]
;;     (loop [i 0
;;            acc! (transient [])]
;;       (if (= i len)
;;         (persistent! acc!)
;;         (let [subv
;;               (subvec limits (inc i))
;;               idx
;;               (mod (quot n (reduce * subv)) (get limits i))]
;;           (recur (inc i) (conj! acc! idx)))))))


;; ;; TODO: refactor
;; (defn prepare-dims
;;   [[dim & dims]]
;;   (if (and dim (seq dims))
;;     (vec (repeat dim (prepare-dims dims)))
;;     []))


;; ;; TODO: refactor
;; (defn decode-binary-array
;;   [bytes field enc]

;;   (println (vec bytes))

;;   (let [bb
;;         (bb/wrap bytes)

;;         levels
;;         (bb/read-int32 bb)

;;         _
;;         (bb/read-int32 bb)

;;         oid
;;         (bb/read-int32 bb)

;;         pairs
;;         (loop [i 0
;;                acc []]
;;           (if (= i levels)
;;             acc
;;             (let [len
;;                   (bb/read-int32 bb)
;;                   dim
;;                   (bb/read-int32 bb)]
;;               (recur (inc i)
;;                      (conj acc [len dim])))))

;;         dims
;;         (mapv first pairs)

;;         matrix
;;         (prepare-dims dims)

;;         total
;;         (reduce * dims)]

;;     (loop [i 0
;;            acc (prepare-dims dims)]

;;       (if (= i total)
;;         acc
;;         (let [len
;;               (bb/read-int32 bb)

;;               item
;;               (when-not (= len -1)
;;                 (decode-binary (bb/read-bytes bb len)
;;                                {:type-id oid
;;                                 :format const/FORMAT_BINARY}
;;                                enc))

;;               path
;;               (get-dims dims i)]

;;           ;; TODO: refactor
;;           (recur (inc i)
;;                  (update-in acc (butlast path) conj item)))))))


;; #_
;; (defmethod decode-binary oid/INT2_ARRAY
;;   [^bytes bytes field enc]
;;   (decode-binary-array bytes field enc))


;; #_
;; (defmethod decode-binary oid/INT4_ARRAY
;;   [^bytes bytes field enc]
;;   (decode-binary-array bytes field enc))

;; #_
;; (defmethod decode-binary oid/INT8_ARRAY
;;   [^bytes bytes field enc]
;;   (decode-binary-array bytes field enc))

;; #_
;; (defmethod decode-binary oid/FLOAT4_ARRAY
;;   [^bytes bytes field enc]
;;   (decode-binary-array bytes field enc))

;; #_
;; (defmethod decode-binary oid/FLOAT8_ARRAY
;;   [^bytes bytes field enc]
;;   (decode-binary-array bytes field enc))

;; #_
;; (defmethod decode-binary oid/TEXT_ARRAY
;;   [^bytes bytes field enc]
;;   (decode-binary-array bytes field enc))


;; ;;
;; ;; The main entry point
;; ;;

;; (defn decode
;;   [bytes
;;    {:as field field-format :format}
;;    enc]
;;   (when (some? bytes)
;;     (case (int field-format)
;;       0 (decode-text bytes field enc)
;;       1 (decode-binary bytes field enc))))
