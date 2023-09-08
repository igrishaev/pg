;; https://github.com/pgjdbc/pgjdbc/blob/135be5a4395033a4ba23a1dd70ad76e0bd443a8d/pgjdbc/src/main/java/org/postgresql/jdbc/ArrayDecoding.java#L498

(ns pg.decode.bin.array
  (:require
   [pg.bb :as bb]
   [pg.oid :as oid]
   [pg.out :as out]
   [pg.decode.bin.core
    :refer [-decode]]))


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


(defn get-matrix
  [[dim & dims]]
  (when dim
    (if (seq dims)
      (vec (repeat dim (get-matrix dims)))
      (vec (repeat dim nil)))))


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

         matrix
         (get-matrix dims)]

     (loop [i 0
            idx (tick)
            matrix (get-matrix dims)]

       (if (= i total)
         matrix

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

           (recur (inc i)
                  (tick idx)
                  (assoc-in matrix idx obj))))))))


;;
;; Arrays
;;

(doseq [oid oid/array-oids]
  (defmethod -decode oid
    [buf _ opt]
    (decode-array buf opt)))
