;; https://github.com/pgjdbc/pgjdbc/blob/135be5a4395033a4ba23a1dd70ad76e0bd443a8d/pgjdbc/src/main/java/org/postgresql/jdbc/ArrayDecoding.java#L498

(ns pg.decode.bin.array
  (:require
   [pg.bb :as bb]
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
                  (bb/read-int32 bb)
                  _
                  (bb/read-int32 bb)]
              (recur (inc i)
                     (conj acc dim)))))




        ])



  )
