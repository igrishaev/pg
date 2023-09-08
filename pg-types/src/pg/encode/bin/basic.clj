(ns pg.encode.bin.basic
  (:import
   clojure.lang.Symbol
   java.util.UUID)
  (:require
   [pg.oid :as oid]
   [pg.bytes :as bytes]
   [pg.encode.bin.core
    :refer [expand
            get-encoding
            -encode]]
   [pg.oid :as oid]))


;;
;; Symbol
;;

(expand [Symbol nil
         Symbol oid/text
         Symbol oid/varchar]
  [value oid opt]
  (-encode (str value) oid opt))


;;
;; String
;;

(expand [String nil
         String oid/text
         String oid/varchar]
  [^String value oid opt]
  (.getBytes value ^String (get-encoding opt)))


;;
;; Character
;;

(expand [Character nil
         Character oid/text
         Character oid/varchar]
  [^Character value oid opt]
  (.getBytes (str value) (get-encoding opt)))


;;
;; Long
;;

(expand [Long nil
         Long oid/int8]
  [value oid opt]
  (bytes/int64->bytes value))

(expand [Long oid/int4]
  [value oid opt]
  (bytes/int32->bytes (int value)))

(expand [Long oid/int2]
  [value oid opt]
  (bytes/int16->bytes (short value)))

(expand [Long oid/float4]
  [value oid opt]
  (-> value
      (Float/floatToIntBits)
      (bytes/int32->bytes)))

(expand [Long oid/float8]
  [value oid opt]
  (-> value
      (Double/doubleToLongBits)
      (bytes/int64->bytes)))


;;
;; Integer
;;

(expand [Integer oid/int8]
  [value oid opt]
  (bytes/int64->bytes (long value)))

(expand [Integer nil
         Integer oid/int4]
  [value oid opt]
  (bytes/int32->bytes value))

(expand [Integer oid/int2]
  [value oid opt]
  (bytes/int16->bytes (short value)))

(expand [Integer oid/float4]
  [value oid opt]
  (-> value
      (Float/floatToIntBits)
      (bytes/int32->bytes)))

(expand [Integer oid/float8]
  [value oid opt]
  (-> value
      (Double/doubleToLongBits)
      (bytes/int64->bytes)))


;;
;; Short
;;

(expand [Short oid/int8]
  [value oid opt]
  (bytes/int64->bytes (long value)))

(expand [Short oid/int4]
  [value oid opt]
  (bytes/int32->bytes (int value)))

(expand [Short nil
         Short oid/int2]
  [value oid opt]
  (bytes/int16->bytes value))

(expand [Short oid/float4]
  [value oid opt]
  (-> value
      (Float/floatToIntBits)
      (bytes/int32->bytes)))

(expand [Short oid/float8]
  [value oid opt]
  (-> value
      (Double/doubleToLongBits)
      (bytes/int64->bytes)))


;;
;; Bool
;;

(expand [Boolean nil
         Boolean oid/bool]
  [value oid opt]
  (case value
    true (byte-array [(byte 1)])
    false (byte-array [(byte 0)])))


;;
;; Byte
;;

(expand [Byte nil
         Byte oid/int2]
  [value oid opt]
  (bytes/int16->bytes (short value)))


(expand [Byte oid/int4]
  [value oid opt]
  (bytes/int32->bytes (int value)))


(expand [Byte oid/int8]
  [value oid opt]
  (bytes/int64->bytes (long value)))


;;
;; Float
;;

(expand [Float nil
         Float oid/float4]
  [value oid opt]
  (-> (Float/floatToIntBits value)
      (bytes/int32->bytes)))

(expand [Float oid/float8]
  [value oid opt]
  (-> value
      (Double/doubleToLongBits)
      (bytes/int64->bytes)))


;;
;; Double
;;

(expand [Double nil
         Double oid/float8]
  [value oid opt]
  (-> (Double/doubleToLongBits value)
      (bytes/int64->bytes)))

(expand [Double oid/float4]
  [value oid opt]
  (-> (Float/floatToIntBits (float value))
      (bytes/int32->bytes)))


;;
;; UUID
;;

(expand [UUID nil
         UUID oid/uuid]
  [^UUID value oid opt]
  (let [most-bits
        (.getMostSignificantBits value)

        least-bits
        (.getLeastSignificantBits value)]

    (byte-array
     (-> []
         (into (bytes/int64->bytes most-bits))
         (into (bytes/int64->bytes least-bits))))))

(expand [String oid/uuid]
  [value oid opt]
  (-encode (UUID/fromString value) oid opt))

(expand [UUID oid/text]
  [value oid opt]
  (-encode (str value) oid opt))
