(ns pg.decode.txt
  (:refer-clojure :exclude [extend])
  (:import
   java.time.ZoneId
   java.time.LocalDate
   java.time.LocalTime
   java.time.Instant
   java.time.format.DateTimeFormatter
   java.util.UUID)
  (:require
   [clojure.template :refer [do-template]]
   [clojure.xml :as xml]
   [pg.oid :as oid]))


(defmulti -decode
  (fn [^String _string oid _opt]
    oid))


(defmethod -decode :default
  [string _ _]
  string)


(defmacro extend
  {:style/indent 1}
  [oid's binding & body]
  `(do-template [oid#]
                (defmethod -decode oid#
                  ~binding
                  ~@body)
                ~@oid's))


(extend [oid/uuid]
  [string _ _]
  (UUID/fromString string))


(extend [oid/text oid/varchar]
  [string _ _]
  string)


(extend [oid/char]
  [^String string _ _]
  (.charAt string 0))


(extend [oid/bool]
  [string _ _]
  (case string
    "t" true
    "f" false))


(extend [oid/int2]
  [^String string _ _]
  (Short/parseShort string))


(extend [oid/int4]
  [string _ _]
  (Integer/parseInt string))


(extend [oid/int8]
  [string _ _]
  (Long/parseLong string))


(extend [oid/float4]
  [string _ _]
  (Float/parseFloat string))


(extend [oid/float8]
  [string _ _]
  (Double/parseDouble string))


#_
(extend [oid/xml]
  [string _ _]
  ...)


(extend [oid/numeric]
  [string _ _]
  (bigdec string))


(def ^DateTimeFormatter dtfz
  (-> "yyyy-MM-dd HH:mm:ss.nx"
      (DateTimeFormatter/ofPattern)
      (.withZone (ZoneId/of "UTC"))))


(extend [oid/timestamptz]
  [string _ _]
  (->> string
       (.parse dtfz)
       (Instant/from)))


(extend [oid/date]
  [string _ _]
  (LocalDate/parse string))


(extend [oid/time]
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
