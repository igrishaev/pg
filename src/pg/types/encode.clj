(ns pg.types.encode
  #_
  (:import
   java.util.UUID
   java.time.ZoneId
   java.time.format.DateTimeFormatter
   java.time.Instant)
  (:require
   [pg.error :as e]
   [pg.codec :as codec]

   ;; [clojure.java.io :as io]
   ;; [clojure.xml :as xml]
   ;; [clojure.string :as str]
   ))




(defprotocol IEncode
  :extend-via-metadata true
  (encode-text ^String [this ^String enc])
  (encode-binary ^bytes [this ^String enc]))


(extend-protocol IEncode

  Object

  (encode-text [this enc]
    (codec/str->bytes (pr-str this) enc))

  (encode-binary [this enc]
    (e/error! "Don't know how to binary encode this value"
              {:this this
               :enc enc
               :in ::here}))

  Boolean

  (encode-text [this enc]
    (if this "t" "f"))

  (encode-binary [this enc]
    (if this
      (byte-array [1])
      (byte-array [0])))

  String

  (encode-text [this enc]
    (codec/str->bytes this enc))

  (encode-binary [this enc]
    (codec/str->bytes this enc))

  Integer

  (encode-text [this enc]
    (codec/str->bytes (str this) enc))

  (encode-binary [this enc]
    #_
    (codec/str->bytes this enc))

  ;; ArrayList
  ;; Map
  ;; UUID

  ;; Timestamp
  ;; Time
  ;; Date

  ;; IPersistentCollection

  ;; Symbol
  ;; Keyword

  ;; nil

  ;; Point
  ;; Box
  ;; Line
  ;; LineSegment
  ;; Circle
  ;; Polygon

  ;; bytes

  ;; Inet
  ;; Cidr

  ;; Char
  ;; Long
  ;; Float
  ;; Double
  ;; BigDecimal
  ;; Number

  ;; (encode-text [this enc]
  ;;   (codec/str->bytes (str this) enc))
  ;; (encode-binary [this enc]
  ;;   nil)



  )
