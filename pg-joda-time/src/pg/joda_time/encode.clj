(ns pg.joda-time.encode
  (:import
   java.time.Instant
   org.joda.time.DateTime)
  (:require
   [pg.oid :as oid]
   [pg.encode.bin.core :as bin]
   [pg.encode.txt.core :as txt]))


;; bin

(bin/expand [DateTime nil
             DateTime oid/timestamptz
             DateTime oid/timestamp
             DateTime oid/date]
  [^DateTime value oid opt]
  (-> value
      (.getMillis)
      (Instant/ofEpochMilli)
      (bin/-encode oid opt)))

;; txt

(txt/expand [DateTime nil
             DateTime oid/timestamptz
             DateTime oid/timestamp
             DateTime oid/date]
  [^DateTime value oid opt]
  (-> value
      (.getMillis)
      (Instant/ofEpochMilli)
      (txt/-encode oid opt)))
