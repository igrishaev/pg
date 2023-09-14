(ns pg.encode.bin.array
  (:import
   clojure.lang.Sequential)
  (:require
   [pg.coll :as coll]
   [pg.out :as out]
   [pg.oid :as oid]
   [pg.types.array :as array]
   [pg.encode.bin.core :refer [expand
                               -encode]]))


(defn matrix-dims [matrix]
  (loop [dims []
         m matrix]
    (if (sequential? m)
      (recur (conj dims (count m))
             (first m))
      (not-empty dims))))


(defn encode-array
  [matrix oid opt]

  (let [dims
        (matrix-dims matrix)

        dim-count
        (count dims)

        total
        (apply * dims)

        items
        (flatten matrix)

        has-nulls
        (if (some nil? items)
          1
          0)

        out
        (doto (out/create)
          (out/write-int32 dim-count)
          (out/write-int32 has-nulls)
          (out/write-int32 oid))]

    (coll/do-seq [dim dims]
      (out/write-int32 out dim)
      (out/write-int32 out 1))

    (coll/do-seq [item items]
      (if (nil? item)
        (out/write-int32 out -1)
        (let [buf ^bytes (-encode item oid opt)
              len (alength buf)]
          (out/write-int32 out len)
          (out/write-bytes out buf))))

    (out/array out)))


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
