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
  (fn [^String _string oid _opt]
    oid))


(defmethod -decode :default
  [string _ _]
  string)


(defmethod -decode oid/uuid
  [string _ _]
  (UUID/fromString string))


(defmethod -decode oid/text
  [string _ _]
  string)


(defmethod -decode oid/varchar
  [string _ _]
  string)


(defmethod -decode oid/char
  [string _ _]
  (first string))


(defmethod -decode oid/bool
  [string _ _]
  (case string
    "t" true
    "f" false))


(defmethod -decode oid/int2
  [string _ _]
  (Short/parseShort string))


(defmethod -decode oid/int4
  [string _ _]
  (Integer/parseInt string))


(defmethod -decode oid/int8
  [string _ _]
  (Long/parseLong string))


(defmethod -decode oid/float4
  [string _ _]
  (Float/parseFloat string))


(defmethod -decode oid/float8
  [string _ _]
  (Double/parseDouble string))


#_
(defmethod -decode oid/xml
  [string _ _]
  ...)


(defmethod -decode oid/numeric
  [string _ _]
  (bigdec string))


(def ^DateTimeFormatter dtfz
  (-> "yyyy-MM-dd HH:mm:ss.nx"
      (DateTimeFormatter/ofPattern)
      (.withZone (ZoneId/of "UTC"))))


(defmethod -decode oid/timestamptz
  [string _ _]
  (->> string
       (.parse dtfz)
       (Instant/from)))


(defmethod -decode oid/date
  [string _ _]
  (LocalDate/parse string))


(defmethod -decode oid/time
  [string _ _]
  (LocalTime/parse string))


;;
;; API
;;

(defn decode

  ([string oid]
   (-decode string oid nil))

  ([string oid opt]
   (-decode string oid opt)))
