(ns pg.joda-time.decode
  (:import
   java.time.ZoneOffset
   java.time.OffsetDateTime
   java.time.LocalDateTime
   java.time.LocalDate
   java.time.Instant
   org.joda.time.DateTime)
  (:require
   [pg.oid :as oid]
   [pg.decode.bin :as bin]
   [pg.decode.txt :as txt]))


(defmacro Instant->DateTime [obj]
  `(-> ~obj
       (.toEpochMilli)
       (DateTime.)))


;;
;; Bin
;;

(bin/expand [oid/timestamptz]
  [value oid opt]

  (let [^OffsetDateTime decoded
        (bin/-decode value oid opt)]

    (-> decoded
        (.toInstant)
        (Instant->DateTime))))


(bin/expand [oid/timestamp]
  [value oid opt]

  (let [^LocalDateTime decoded
        (bin/-decode value oid opt)]

    (-> decoded
        (.atOffset ZoneOffset/UTC)
        (.toInstant)
        (Instant->DateTime))))


(bin/expand [oid/date]
  [value oid opt]

  (let [^LocalDate decoded
        (bin/-decode value oid opt)]

    (-> decoded
        (.atStartOfDay ZoneOffset/UTC)
        (.toOffsetDateTime)
        (.toInstant)
        (Instant->DateTime))))


;;
;; Txt
;;

(txt/expand [oid/timestamptz]
  [value oid opt]

  (let [^OffsetDateTime decoded
        (txt/-decode value oid opt)]

    ;; TODO
    (-> decoded
        (.toInstant)
        (Instant->DateTime))))


(txt/expand [oid/timestamp]
  [value oid opt]

  (let [^LocalDateTime decoded
        (txt/-decode value oid opt)]

    (-> decoded
        (.atOffset ZoneOffset/UTC)
        (.toInstant)
        (Instant->DateTime))))


(txt/expand [oid/date]
  [value oid opt]

  (let [^LocalDate decoded
        (txt/-decode value oid opt)]

    (-> decoded
        (.atStartOfDay ZoneOffset/UTC)
        (.toOffsetDateTime)
        (.toInstant)
        (Instant->DateTime))))
