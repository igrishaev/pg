(ns pg.encode.txt.basic
  (:import
   clojure.lang.BigInt
   clojure.lang.Symbol
   java.math.BigDecimal
   java.math.BigInteger
   java.util.UUID)
  (:require
   [pg.encode.txt.core
    :refer [expand]]
   [pg.oid :as oid]))


;;
;; Symbol
;;

(expand [Symbol nil
         Symbol oid/text
         Symbol oid/varchar]
  [value oid opt]
  (str value))


;;
;; String
;;

(expand [String nil
         String oid/text
         String oid/varchar]
  [^String value oid opt]
  value)


;;
;; Character
;;

(expand [Character nil
         Character oid/text
         Character oid/varchar]
  [^Character value oid opt]
  (str value))


;;
;; Long, Integer, Short
;; Double, Float
;; BigDecimal, BigInteger, BigInt
;;

(expand [Long nil
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
         Short oid/int2

         Double nil
         Double oid/float8
         Double oid/float4

         Float nil
         Float oid/float8
         Float oid/float4

         BigDecimal nil
         BigDecimal oid/numeric
         BigDecimal oid/int2
         BigDecimal oid/int4
         BigDecimal oid/int8
         BigDecimal oid/float4
         BigDecimal oid/float8

         BigInteger nil
         BigInteger oid/numeric
         BigInteger oid/int2
         BigInteger oid/int4
         BigInteger oid/int8

         BigInt nil
         BigInt oid/numeric
         BigInt oid/int2
         BigInt oid/int4
         BigInt oid/int8]
  [value _ _]
  (str value))


;;
;; Boolean
;;

(expand [Boolean nil
         Boolean oid/bool]
  [^Boolean value oid opt]
  (case value
    true "t"
    false "f"))


;;
;; OID
;;

(expand [Short      oid/oid
         Integer    oid/oid
         Long       oid/oid
         BigInt     oid/oid
         BigDecimal oid/oid]
  [value oid opt]
  (str value))


;;
;; Name
;;

(expand [String oid/name]
  [value oid opt]
  value)


;;
;; UUID
;;

(expand [UUID nil
         UUID oid/uuid
         UUID oid/text
         UUID oid/varchar]
  [^UUID value oid opt]
  (str value))
