;; https://github.com/pgjdbc/pgjdbc/blob/135be5a4395033a4ba23a1dd70ad76e0bd443a8d/pgjdbc/src/main/java/org/postgresql/jdbc/ArrayDecoding.java#L498

(ns pg.decode.bin.array
  (:require
   [pg.bb :as bb]
   [pg.oid :as oid]
   [pg.out :as out]
   [pg.decode.bin.core
    :refer [expand
            -decode]]))


#_
[0, 0, 0, 1,  ;; dim count
 0, 0, 0, 0,  ;; nuls
 0, 0, 0, 23, ;; oid
 0, 0, 0, 3,  ;; dim1
 0, 0, 0, 1,  ;; ?
 0, 0, 0, 4,  ;; len
 0, 0, 0, 1,  ;; val1
 0, 0, 0, 4,  ;; len
 0, 0, 0, 2,  ;; val2
 0, 0, 0, 4,  ;; len
 0, 0, 0, 3   ;; val3
 ]




(defn ticker [dims]
  (fn -this
    ([]
     (vec (repeat (count dims) 0)))

    ([curr]
     (-this curr (dec (count curr))))
    ([curr i]
     (when-not (= i -1)
       (if (= (get dims i) (get curr i))
         (recur (assoc curr i 0) (dec i))
         (update curr i inc))))))



(defn decode-array

  ([buf]
   (decode-array buf nil))

  ([^bytes buf opt]

   (let [bb
         (bb/wrap buf)

         dim-count
         (bb/read-int32 bb)

         has-nulls?
         (case (bb/read-int32 bb)
           1 true
           0 false)

         oid
         (bb/read-int32 bb)

         dims
         (loop [i 0
                acc []]
           (if (= i dim-count)
             acc
             (let [dim
                   (bb/read-int32 bb)]
               (bb/skip bb 4)
               (recur (inc i)
                      (conj acc dim)))))

         total
         (apply * dims)

         tick
         (ticker (mapv dec dims))

         array
         (apply make-array Object dims)]

     (loop [i 0 idx (tick)]
       (when-not (= i total)

         (let [len
               (bb/read-int32 bb)

               null?
               (= len -1)

               buf
               (when-not null?
                 (bb/read-bytes bb len))

               obj
               (when-not null?
                 (-decode buf oid opt))]

           (apply aset array (conj idx obj))
           (recur (inc i) (tick idx)))))

     array)))


;; TODO: vectors?


;;
;; Arrays
;;

(expand [oid/_bool
         oid/_text
         oid/_int2
         oid/_int4
         oid/_int8]
  [buf _ opt]
  (decode-array buf opt))


#_
(defn encode-array [matrix oid]


  (doto (out/create)

    dim-count
    has-nulls
    oid
    [dim 1]+
    [len/-1 buf]+

    )

  )
