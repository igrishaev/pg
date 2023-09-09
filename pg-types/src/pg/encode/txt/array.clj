(ns pg.encode.txt.array
  (:require
   [clojure.string :as str]
   [pg.hint :as hint]
   [pg.out :as out]
   [pg.oid :as oid]
   [pg.encode.txt.core
    :refer [expand -encode]]))


(defn quote'''' ^String [^String string]
  (format "\"%s\"" (str/replace string #"\"" "\\\\\"")))


;; TODO: recursion
(defn ->string [data oid opt]
  (if (sequential? data)
    (format "{%s}" (->> data
                        (mapv (fn [x]
                                (->string x oid opt)))
                        (str/join ",")))
    (quote'''' (str data)

     #_('-encode data oid opt))))


(defn encode-array

  ([matrix]
   (encode-array matrix nil nil))

  ([matrix oid]
   (encode-array matrix oid nil))

  ([matrix oid opt]
   (->string matrix oid opt)))
