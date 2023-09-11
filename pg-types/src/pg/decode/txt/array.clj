(ns pg.decode.txt.array
  (:import
   java.io.Reader
   java.io.StringReader
   java.io.PushbackReader))


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


(defmacro ->char [r]
  `(if (= ~r -1)
     (throw (new Exception "EOF"))
     (char ~r)))


(defn read-non-quoted-string [^PushbackReader in]
  (let [sb (new StringBuilder)]
    (loop []
      (let [r (.read in)
            c (->char r)]
        (case c
          (\, \})
          (do
            (.unread in r)
            (str sb))
          (do
            (.append sb c)
            (recur)))))))


(defn read-quoted-string [^Reader in]
  (.read in) ;; skip the leading "
  (let [sb (new StringBuilder)]
    (loop []
      (let [r (.read in)
            c (->char r)]
        (case c

          \\
          (let [r+ (.read in)
                c+ (->char r+)]
            (case c+
              (\\ \")
              (do (.append sb c+)
                  (recur))
              ;; else
              (throw (new Exception "unexpected \\ character"))))

          \"
          (let [line (str sb)]
            (when-not (= line "NULL")
              line))

          ;; else
          (do
            (.append sb c)
            (recur)))))))


(defn aaa [value]

  (let [in (->> value
                (new StringReader)
                (new PushbackReader))]

    (loop [dims []
           pos -1
           res []]

      (let [r (.read in)]
        (if (= -1 r)
          res

          (let [c (char r)]

            (case c

              \{
              (recur dims pos res)

              \}
              (recur dims pos res)

              \,
              (recur dims pos res)

              \"
              (do
                (.unread in r)
                (let [string (read-quoted-string in)]
                  (recur dims pos (conj res string))))

              ;; else
              (do
                (.unread in r)
                (let [string (read-non-quoted-string in)]
                  (recur dims pos (conj res string)))))))))))
