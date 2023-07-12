(ns pg.decode.txt
  (:refer-clojure :exclude [extend])
  (:import
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


(defmacro extend
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

(extend [oid/uuid]
  [string _ _]
  (UUID/fromString string))


;;
;; Text
;;

(extend [oid/text oid/varchar]
  [string _ _]
  string)


(extend [oid/char]
  [^String string _ _]
  (.charAt string 0))


;;
;; Boolean
;;

(extend [oid/bool]
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


(extend [oid/numeric]
  [string _ _]
  (bigdec string))


;;
;; Date & time
;;

(extend [oid/timestamptz]
  [string _ opt]
  (datetime/parse-timestampz string opt))


(extend [oid/timestamp]
  [string _ opt]
  (datetime/parse-timestamp string opt))


(extend [oid/date]
  [string _ opt]
  (datetime/parse-date string opt))


(extend [oid/timetz]
  [string _ opt]
  (datetime/parse-timetz string opt))


(extend [oid/time]
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
