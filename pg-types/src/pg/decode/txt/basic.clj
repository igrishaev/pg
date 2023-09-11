(ns pg.decode.txt.basic
  (:import
   java.math.BigDecimal
   java.util.UUID)
  (:require
   [pg.decode.txt.core :refer [expand]]
   [pg.oid :as oid]))


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


(expand [oid/int4 oid/oid]
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
