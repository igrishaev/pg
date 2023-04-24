(ns pg.encode.bin
  (:import
   clojure.lang.Keyword
   clojure.lang.Symbol
   java.time.Duration
   java.time.Instant
   java.time.LocalDate
   java.time.ZoneId
   java.time.ZoneOffset
   java.util.Date
   java.util.UUID)
  (:require
   [pg.bytes.array :as array]
   [pg.oid :as oid]))


(defmulti -encode
  (fn [value oid _]
    [(type value) oid]))


(defmethod -encode :default
  [value oid opt]
  (throw (ex-info "Cannot binary encode a value"
                  {:type ::error
                   :value value
                   :oid oid
                   :opt opt})))


;;
;; Symbol
;;

(defmethod -encode [Symbol oid/TEXT]
  [^Symbol value oid opt]
  (-encode (str value) oid opt))


(defmethod -encode [Symbol oid/VARCHAR]
  [^Symbol value oid opt]
  (-encode (str value) oid opt))


;;
;; String
;;

(defmethod -encode [String oid/TEXT]
  [^String value _ _]
  (.getBytes value "UTF-8"))


(defmethod -encode [String oid/VARCHAR]
  [^String value oid opt]
  (-encode value oid/TEXT opt))


;;
;; Character
;;

(defmethod -encode [Character oid/TEXT]
  [^Character value oid opt]
  (-encode (str val) oid opt))


(defmethod -encode [Character oid/VARCHAR]
  [^Character value oid opt]
  (-encode value oid/TEXT opt))


;;
;; Long
;;

(defmethod -encode [Long oid/INT8]
  [^Long value _ _]
  (array/arr64 value))


(defmethod -encode [Long oid/INT4]
  [^Long value oid opt]
  (-encode (int value) oid opt))


(defmethod -encode [Long oid/INT2]
  [^Long value oid opt]
  (-encode (short value) oid opt))


;;
;; Integer
;;

(defmethod -encode [Integer oid/INT8]
  [^Integer value oid opt]
  (-encode (long value) oid opt))


(defmethod -encode [Integer oid/INT4]
  [^Integer value oid opt]
  (array/arr32 value))


(defmethod -encode [Integer oid/INT2]
  [^Integer value oid opt]
  (-encode (short value) oid opt))


;;
;; Short
;;

(defmethod -encode [Short oid/INT8]
  [^Short value oid opt]
  (-encode (long value) oid opt))


(defmethod -encode [Short oid/INT4]
  [^Short value oid opt]
  (-encode (int value) oid opt))


(defmethod -encode [Short oid/INT2]
  [^Short value oid opt]
  (array/arr16 value))


;;
;; Bool
;;

(defmethod -encode [Boolean oid/BOOL]
  [^Boolean value _ _]
  (let [b
        (case value
          true 1
          false 0)]
    (byte-array [b])))


;;
;; Float
;;

(defmethod -encode [Float oid/FLOAT4]
  [^Float value oid opt]
  (-> (Float/floatToIntBits value)
      (array/arr32)))


(defmethod -encode [Float oid/FLOAT8]
  [^Float value oid opt]
  (-encode (double value) oid opt))


;;
;; Double
;;

(defmethod -encode [Double oid/FLOAT4]
  [^Double value oid opt]
  (-encode (float value) oid opt))


(defmethod -encode [Double oid/FLOAT8]
  [^Double value oid opt]
  (-> (Double/longBitsToDouble value)
      (array/arr64)))


;;
;; UUID
;;

(defmethod -encode [UUID oid/UUID]
  [^UUID value oid opt]

  (let [most-bits
        (.getMostSignificantBits value)

        least-bits
        (.getLeastSignificantBits value)]

    (byte-array
     (-> []
         (into (array/arr64 most-bits))
         (into (array/arr64 least-bits))))))


;;
;; Instant
;;

(def ^Duration PG_EPOCH_DIFF
  (Duration/between Instant/EPOCH
                    (-> (LocalDate/of 2000 1 1)
                        (.atStartOfDay)
                        (.toInstant ZoneOffset/UTC))))


(defmethod -encode [Instant oid/TIMESTAMP]

  [^Instant value _ _]

  (let [seconds
        (- (.getEpochSecond value)
           (.toSeconds PG_EPOCH_DIFF))

        nanos
        (.getNano value)]

    (array/arr64
     (+ (* seconds 1000 1000) nanos))))


(defmethod -encode [Instant oid/DATE]
  [^Instant value oid opt]
  (let [local-date
        (LocalDate/ofInstant value
                             (ZoneId/systemDefault))]
    (-encode local-date oid opt)))


;;
;; Date
;;

(defmethod -encode [Date oid/DATE]
  [^Date value oid opt]
  (let [local-date
        (LocalDate/ofInstant (.toInstant value)
                             (ZoneId/systemDefault))]
    (-encode local-date oid opt)))


;;
;; LocalDate
;;

(defmethod -encode [LocalDate oid/DATE]
  [^LocalDate value oid opt]
  (let []
    (array/arr32
     (- (.toEpochDay value)
        (.toDays PG_EPOCH_DIFF)))))

;;
;; API
;;

(def defaults
  {String    oid/TEXT
   Instant   oid/TIMESTAMP
   Date      oid/TIMESTAMP
   LocalDate oid/DATE
   Symbol    oid/TEXT
   Long      oid/INT8
   Float     oid/FLOAT4
   Double    oid/FLOAT8
   UUID      oid/UUID})


(defn encode
  ([value]
   (encode value (get defaults (type value))))

  ([value oid]
   (encode value oid nil))

  ([value oid opt]
   (-encode value oid opt)))
