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
  (fn [oid string]
    oid))


(defmethod -decode :default
  [_ string]
  string)


(defmethod -decode oid/uuid
  [_ string]
  (UUID/fromString string))


(defmethod -decode oid/text
  [_ string]
  string)


(defmethod -decode oid/varchar
  [_ string]
  string)


(defmethod -decode oid/char
  [_ string]
  (first string))


(defmethod -decode oid/bool
  [_ string]
  (case string
    "t" true
    "f" false))


(defmethod -decode oid/int2
  [_ string]
  (Short/parseShort string))


(defmethod -decode oid/int4
  [_ string]
  (Integer/parseInt string))


(defmethod -decode oid/int8
  [_ string]
  (Long/parseLong string))


(defmethod -decode oid/float4
  [_ string]
  (Float/parseFloat string))


(defmethod -decode oid/float8
  [_ string]
  (Double/parseDouble string))


#_
(defmethod -decode oid/xml
  [_ string]
  (Double/parseDouble string))


(defmethod -decode oid/numeric
  [_ string]
  (bigdec string))


(def ^DateTimeFormatter dtfz
  (-> "yyyy-MM-dd HH:mm:ss.nx"
      (DateTimeFormatter/ofPattern)
      (.withZone (ZoneId/of "UTC"))))


(defmethod -decode oid/timestamptz
  [_ string]
  (->> string
       (.parse dtfz)
       (Instant/from)))


(defmethod -decode oid/date
  [_ string]
  (LocalDate/parse string))


(defmethod -decode oid/time
  [_ string]
  (LocalTime/parse string))
