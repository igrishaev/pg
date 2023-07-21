(ns pg.decode.txt
  (:import
   java.math.BigDecimal
   java.util.UUID)
  (:require
   [pg.decode.txt.datetime :as datetime]
   [clojure.template :refer [do-template]]
   [clojure.xml :as xml]
   [pg.oid :as oid]))


(defmulti -decode
  (fn [^String _string oid _opt]
    oid))


(defmethod -decode :default
  [string _ _]
  string)


(defmacro expand
  {:style/indent 1}
  [oid's binding & body]
  `(do-template [oid#]
                (defmethod -decode oid#
                  ~binding
                  ~@body)
                ~@oid's))


;;
;; UUID
;;

(expand [oid/uuid]
  [string _ _]
  (UUID/fromString string))


;;
;; Text
;;

(expand [oid/text oid/varchar]
  [string _ _]
  string)


(expand [oid/char]
  [^String string _ _]
  (.charAt string 0))


;;
;; Boolean
;;

(expand [oid/bool]
  [string oid opt]
  (case string
    "t" true
    "f" false
    ;; else
    (throw
     (ex-info
      (format "cannot parse bool: %s" string)
      {:string string
       :oid oid
       :opt opt}))))


;;
;; Numbers
;;

(expand [oid/int2]
  [^String string _ _]
  (Short/parseShort string))


(expand [oid/int4]
  [string _ _]
  (Integer/parseInt string))


(expand [oid/int8]
  [string _ _]
  (Long/parseLong string))


(expand [oid/float4]
  [string _ _]
  (Float/parseFloat string))


(expand [oid/float8]
  [string _ _]
  (Double/parseDouble string))


(expand [oid/numeric]
  [^String string _ _]
  (new BigDecimal string))


;;
;; Date & time
;;

(expand [oid/timestamptz]
  [string _ opt]
  (datetime/parse-timestampz string opt))


(expand [oid/timestamp]
  [string _ opt]
  (datetime/parse-timestamp string opt))


(expand [oid/date]
  [string _ opt]
  (datetime/parse-date string opt))


(expand [oid/timetz]
  [string _ opt]
  (datetime/parse-timetz string opt))


(expand [oid/time]
  [string _ opt]
  (datetime/parse-time string opt))


;;
;; API
;;

(defn decode

  ([string oid]
   (-decode string oid nil))

  ([string oid opt]
   (-decode string oid opt)))
