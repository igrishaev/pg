(ns pg.encode.txt.array
  (:import
   clojure.lang.Sequential)
  (:require
   [clojure.string :as str]
   [pg.types.array :as array]
   [pg.out :as out]
   [pg.oid :as oid]
   [pg.encode.txt.core
    :refer [expand -encode]]))


(defn quote-array ^String [^String string]
  (let [len (.length string)
        sb  (new StringBuilder)]
    (.append sb \")
    (loop [i 0]
      (if (= i len)
        (do
          (.append sb \")
          (str sb))
        (let [c (.charAt string i)
              c'
              (case c
                \" "\\\""
                \\ "\\\\"
                c)]
          (.append sb c')
          (recur (inc i)))))))


(defn ->string [data oid opt]
  (cond

    (sequential? data)
    (let [items
          (->> data
               (mapv (fn [x]
                       (->string x oid opt)))
               (str/join ","))]
      (format "{%s}" items))

    (nil? data)
    "NULL"

    :else
    (quote-array (-encode data oid opt))))


(defn encode-array

  ([matrix]
   (encode-array matrix nil nil))

  ([matrix oid]
   (encode-array matrix oid nil))

  ([matrix oid opt]
   (->string matrix oid opt)))


;;
;; Array
;;

(defmethod -encode [Sequential nil]
    [value _ opt]
    (let [oid (array/guess-oid value)]
      (encode-array value oid opt)))


(doseq [oid oid/array-oids]
  (defmethod -encode [Sequential oid]
    [value oid-arr opt]
    (let [oid (oid/array->oid oid-arr)]
      (encode-array value oid opt))))
