(ns pg.decode.txt.array)


(defn assoc-vec [v i x]
  (let [len (count v)
        v (if (nil? v) [] v)]
    (cond
      (= i len)
      (conj v x)
      (< i len)
      (assoc v i x)
      :else
      (throw (new Exception "index error")))))


(defn assoc-vec-in [v [i & ii] x]
  (if ii
    (assoc-vec v i (assoc-vec-in (get v i) ii x))
    (assoc-vec v i x)))


(defn bar [^String string]

  (let [sb (new StringBuilder)
        len (.length string)]

    (loop [i 0
           reading? false
           quote? false
           dims []
           pos -1
           res []]

      (if (= i len)

        res

        (let [c (.charAt string i)]

          (println "---" c (if reading? 't 'f) (if quote? 't 'f) dims pos (str sb) res)

          (case [c reading? quote?]

            [\" false false]
            (recur (inc i) false true dims pos res)

            [\\ true true]
            (let [c+ (.charAt string (inc i))]
              (case c+
                (\" \\)
                (do
                  (.append sb c+)
                  (recur (+ i 2) true quote? dims pos res))))

            [\" true true]
            (recur (inc i) true false dims pos res)

            [\{ false false]
            (let [pos+ (inc pos)]
              (recur (inc i) reading? quote?
                     (if (< (dec (count dims)) pos+)
                       (conj dims 0)
                       dims)
                     pos+
                     res))

            [\{ true true]
            (do
              (.append sb c)
              (recur (inc i) reading? quote? dims pos res))

            ;; commit
            [\, true false]
            (let [line (str sb)]
              (.setLength sb 0)
              (recur (inc i) false false (update dims pos inc) pos
                     (assoc-vec-in res dims line)))

            [\, true true]
            (do
              (.append sb c)
              (recur (inc i) reading? quote? dims pos res))

            [\, false false]
            (recur (inc i) reading? quote? (update dims pos inc) pos res)

            [\} true true]
            (do
              (.append sb c)
              (recur (inc i) reading? quote? dims pos res))

            ;; commit
            [\} true false]
            (let [line (str sb)]
              (.setLength sb 0)
              (recur (inc i) false false (assoc dims pos 0) (dec pos)
                     (assoc-vec-in res dims line)))

            [\} false false]
            (recur (inc i) false false (assoc dims pos 0) (dec pos) res)

            ;; else

            (do
              (.append sb c)
              (recur (inc i) true quote? dims pos res))))))))
