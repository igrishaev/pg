(ns pg.quote
  (:require
   [clojure.string :as str]))


(defn quote-str [string]
  (str \' (str/replace string #"'" "''") \'))
