(ns pg.client.quote
  (:require
   [clojure.string :as str]))


(defn quote'' ^String [^String string]
  (format "\"%s\"" (str/replace string #"\"" "\"\"")))
