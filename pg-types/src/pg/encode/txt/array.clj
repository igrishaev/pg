(ns pg.encode.txt.array
  (:require
   [clojure.string :as str]
   [pg.hint :as hint]
   [pg.out :as out]
   [pg.oid :as oid]
   #_
   [pg.encode.bin.core :refer [expand
                               -encode]]))


(defn quote'''' ^String [^String string]
  (format "\"%s\"" (str/replace string #"\"" "\\\\\"")))


(defn matrix-dims [matrix]
  (loop [dims []
         m matrix]
    (if (sequential? m)
      (recur (conj dims (count m))
             (first m))
      (not-empty dims))))


(defn foo [data]
  (if (sequential? data)
    (format "{%s}" (->> data
                        (mapv foo)
                        (str/join ",")))
    (quote'''' data)))


(defn encode-array

  ([matrix]
   (encode-array matrix nil nil))

  ([matrix oid]
   (encode-array matrix oid nil))

  ([matrix oid opt]
   (with-out-str
     (loop [m matrix]
       (if-let [item (first m)]
         1
         )
       )
     ))

  )
