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
            (let [line (str sb)]
              (when-not (= line "NULL")
                line)))
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
          (str sb)

          ;; else
          (do
            (.append sb c)
            (recur)))))))


(defn parse-array [value]

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
              (let [pos+ (inc pos)]
                (recur (if (< (dec (count dims)) pos+)
                         (conj dims 0)
                         dims)
                       pos+
                       res))

              \}
              (recur (assoc dims pos 0)
                     (dec pos)
                     res)

              \,
              (recur (update dims pos inc)
                     pos
                     res)

              \"
              (do
                (.unread in r)
                (let [string (read-quoted-string in)]
                  (recur dims
                         pos
                         (assoc-vec-in res dims string))))

              ;; else
              (do
                (.unread in r)
                (let [string (read-non-quoted-string in)]
                  (recur dims
                         pos
                         (assoc-vec-in res dims string)))))))))))
