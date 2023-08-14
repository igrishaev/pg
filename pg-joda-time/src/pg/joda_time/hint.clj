(ns pg.joda-time.hint
  (:import
   org.joda.time.DateTime)
  (:require
   [pg.oid :as oid]
   [pg.hint :as hint]))


(hint/add-hint DateTime oid/timestamptz)
