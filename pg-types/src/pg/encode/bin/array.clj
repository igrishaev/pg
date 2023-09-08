(ns pg.encode.bin.array
  (:require
   [pg.bytes :as bytes]
   [pg.bb :as bb]
   [pg.hint :as hint]
   [pg.out :as out]
   [pg.oid :as oid]))


(defn matrix-dims [matrix]
  (loop [dims []
         m matrix]
    (if (sequential? m)
      (recur (conj dims (count m))
             (first m))
      (not-empty dims))))


;; todo throw if nil

(defn encode-array [matrix]

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

    ;; TODO coll
    (doseq [dim dims]
      (out/write-int32 out dim)
      (out/write-int32 out 1))

    ;; TODO coll
    (doseq [item items]
      (if (nil? item)
        (out/write-int32 out -1)
        (let [^bytes buf (byte-array [0 0 0 9]) #_(:encode item oid :opt)
              len (alength buf)]
          (out/write-int32 out len)
          (out/write-bytes out buf))))

    (out/array out)))
