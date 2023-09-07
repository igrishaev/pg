;; https://github.com/pgjdbc/pgjdbc/blob/135be5a4395033a4ba23a1dd70ad76e0bd443a8d/pgjdbc/src/main/java/org/postgresql/jdbc/ArrayDecoding.java#L498

(ns pg.decode.bin.array
  (:require
   [pg.bb :as bb]
   [pg.out :as out]
   ;; [pg.coll :as coll]
   ))


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

#_
[ 0,  0,  0,  2,  ;; dims
  0,  0,  0,  1,  ;; nulls true
  0,  0,  0,  23, ;; oid
  0,  0,  0,  2,  ;; dim1 = 2
  0,  0,  0,  1,  ;; ?
  0,  0,  0,  3,  ;; dom2 = 3
  0,  0,  0,  1,  ;; ?
  0,  0,  0,  4,  ;; len
  0,  0,  0,  1,  ;; 1
  0,  0,  0,  4,  ;; len
  0,  0,  0,  2,  ;; 2
  0,  0,  0,  4,  ;; len
  0,  0,  0,  3,  ;; 3
  0,  0,  0,  4,  ;; len
  0,  0,  0,  4,  ;; 4
 -1, -1, -1, -1,  ;; nul
  0,  0,  0,  4,  ;; len
  0,  0,  0,  6   ;; 6
 ]

;; [3 4 5]
;; 34

;; 34 / 3 = (10, 3)

(defn dim-next
  ([dims curr]
   (foo dims curr (dec (count curr))))

  ([dims curr i]
   (when (= i -1)
     (throw (new Exception "out of boundaries")))
   (if (= (get dims i) (get curr i))
     (recur dims (assoc curr i 0) (dec i))
     (update curr i inc))))


(defn make-ticker [dims]
  (fn -tick
    ([curr]
     (-tick curr (dec (count curr))))
    ([curr i]

     #_
     (when (= i -1)
       (throw (new Exception "out of boundaries")))
     (when (not= i -1)
       (if (= (get dims i) (get curr i))
         (recur (assoc curr i 0) (dec i))
         (update curr i inc))))))



(defn decode-array [^bytes buf]

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

        ticker
        (make-ticker (mapv dec dims))

        array
        (apply make-array Object dims)

        idx
        (vec (repeat dim-count 0))]

    (loop [i 0 idx idx]
      (when (not= i total)
        (let [len
              (bb/read-int32 bb)
              buf
              (when (not= len -1)
                (bb/read-bytes bb len))]

          (apply aset array (conj idx buf))
          (recur (inc i) (ticker idx)))))

    array))


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
