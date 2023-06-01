(ns pg.decode.txt
  (:import
   java.time.ZoneId
   java.time.LocalDate
   java.time.LocalTime
   java.time.Instant
   java.time.format.DateTimeFormatter
   java.util.UUID)
  (:require
   [clojure.xml :as xml]
   [pg.oid :as oid]))


(defmulti -decode
  (fn [oid ^String _string _opt]
    oid))


(defmethod -decode :default
  [_ string _]
  string)


(defmethod -decode oid/uuid
  [_ string _]
  (UUID/fromString string))


(defmethod -decode oid/text
  [_ string _]
  string)


(defmethod -decode oid/varchar
  [_ string _]
  string)


(defmethod -decode oid/char
  [_ string _]
  (first string))


(defmethod -decode oid/bool
  [_ string _]
  (case string
    "t" true
    "f" false))


(defmethod -decode oid/int2
  [_ string _]
  (Short/parseShort string))


(defmethod -decode oid/int4
  [_ string _]
  (Integer/parseInt string))


(defmethod -decode oid/int8
  [_ string _]
  (Long/parseLong string))


(defmethod -decode oid/float4
  [_ string _]
  (Float/parseFloat string))


(defmethod -decode oid/float8
  [_ string _]
  (Double/parseDouble string))


#_
(defmethod -decode oid/xml
  [_ string _]
  ...)


(defmethod -decode oid/numeric
  [_ string _]
  (bigdec string))


(def ^DateTimeFormatter dtfz
  (-> "yyyy-MM-dd HH:mm:ss.nx"
      (DateTimeFormatter/ofPattern)
      (.withZone (ZoneId/of "UTC"))))


(defmethod -decode oid/timestamptz
  [_ string _]
  (->> string
       (.parse dtfz)
       (Instant/from)))


(defmethod -decode oid/date
  [_ string _]
  (LocalDate/parse string))


(defmethod -decode oid/time
  [_ string _]
  (LocalTime/parse string))


(defn decode

  ([oid string]
   (-decode oid string nil))

  ([oid string opt]
   (-decode oid string opt)))
