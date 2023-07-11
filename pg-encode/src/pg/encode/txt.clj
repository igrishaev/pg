(ns pg.encode.txt
  (:refer-clojure :exclude [extend])
  (:require
   [pg.encode.txt.datetime :as datetime]
   [clojure.template :refer [do-template]]
   [pg.oid :as oid])
  (:import
   java.util.Date
   java.time.Instant
   java.util.UUID
   java.util.Locale
   java.util.Formatter
   clojure.lang.Symbol))


(defmulti -encode
  (fn [value oid _]
    [(type value) oid]))


(defmethod -encode :default
  [value oid opt]
  (throw (ex-info "Cannot text-encode a value"
                  {:value value
                   :oid oid
                   :opt opt})))


(defmacro extend
  {:style/indent 1}
  [type-oid's binding & body]
  `(do-template [Type# oid#]
                (defmethod -encode [Type# oid#]
                  ~binding
                  ~@body)
                ~@type-oid's))


(defmacro format-us [value pattern]
  `(String/format Locale/US ~pattern (to-array [~value])))


;;
;; Symbol
;;

(extend [Symbol nil
         Symbol oid/text
         Symbol oid/varchar]
  [value oid opt]
  (str value))


;;
;; String
;;

(extend [String nil
         String oid/text
         String oid/varchar]
  [^String value oid opt]
  value)


;;
;; Character
;;

(extend [Character nil
         Character oid/text
         Character oid/varchar]
  [^Character value oid opt]
  (str value))


;;
;; Long, Integer, Short
;;

(extend [Long nil
         Long oid/int8
         Long oid/int4
         Long oid/int2

         Integer nil
         Integer oid/int8
         Integer oid/int4
         Integer oid/int2

         Short nil
         Short oid/int8
         Short oid/int4
         Short oid/int2]
  [value oid opt]
  (format-us value "%d"))


;;
;; Double, Float
;;

(extend [Double nil
         Double oid/float8
         Double oid/float4

         Float nil
         Float oid/float8
         Float oid/float4]
  [value oid opt]
  (format-us value "%f"))


;;
;; Boolean
;;


(extend [Boolean nil
         Boolean oid/bool]
  [^Boolean value oid opt]
  (case value
    true "t"
    false "f"))


;;
;; UUID
;;

(extend [UUID nil
         UUID oid/uuid
         UUID oid/text
         UUID oid/varchar]
  [^UUID value oid opt]
  (str value))


;;
;; Date & time
;;

(extend [Instant nil
         Instant oid/timestamptz]
  [value _ opt]
  (datetime/Instant-timestamptz value opt))


(extend [Instant oid/timestamp]
  [value _ opt]
  (datetime/Instant-timestamp value opt))


(extend [Instant oid/date]
  [value _ opt]
  (datetime/Instant-date value opt))


(extend [Date nil
         Date oid/timestamptz]
  [value _ opt]
  (datetime/Date-timestamptz value opt))


(extend [Date oid/timestamp]
  [value _ opt]
  (datetime/Date-timestamp value opt))


(extend [Date oid/date]
  [value _ opt]
  (datetime/Date-date value opt))



;;
;; API
;;

(defn encode
  (^String [value]
   (-encode value nil nil))

  (^String [value oid]
   (-encode value oid nil))

  (^String [value oid opt]
   (-encode value oid opt)))
