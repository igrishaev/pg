(ns pg.encode.bin.array
  (:import
   clojure.lang.Sequential)
  (:require
   [pg.coll :as coll]
   [pg.hint :as hint]
   [pg.out :as out]
   [pg.oid :as oid]
   [pg.encode.bin.core :refer [expand
                               -encode]]))


(defn matrix-dims [matrix]
  (loop [dims []
         m matrix]
    (if (sequential? m)
      (recur (conj dims (count m))
             (first m))
      (not-empty dims))))


;; todo throw if nil
;; TODO: guess-oid

(defn encode-array

  ([matrix]
   (encode-array matrix nil))

  ([matrix opt]

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

         oid
         (-> (filter some? items)
             (first)
             (hint/hint))

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

     (out/array out))))


;;
;; Array
;;

(doseq [oid (conj oid/array-oids nil)]
  (defmethod -encode [Sequential oid]
    [value oid-arr opt]
    (encode-array value opt)))
