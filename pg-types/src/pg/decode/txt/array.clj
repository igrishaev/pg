(ns pg.decode.txt.array
  (:require
   [clojure.string :as str]
   [pg.decode.txt.core :refer [expand
                               -decode]]
   [pg.oid :as oid])
  (:import
   java.io.PushbackReader
   java.io.Reader
   java.io.StringReader))


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


(defmacro null-literal? [line]
  `(= (str/lower-case ~line) "null"))


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
              (when-not (null-literal? line)
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


(defn decode-array [string oid opt]

  (let [in (->> string
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
                (let [buf (read-quoted-string in)
                      obj (-decode buf oid opt)]
                  (recur dims
                         pos
                         (assoc-vec-in res dims obj))))

              ;; else
              (do
                (.unread in r)
                (let [buf (read-non-quoted-string in)
                      obj (-decode buf oid opt)]
                  (recur dims
                         pos
                         (assoc-vec-in res dims obj)))))))))))


;;
;; Arrays
;;

(doseq [oid oid/array-oids]
  (defmethod -decode oid
    [string oid-arr opt]
    (let [oid (oid/array->oid oid-arr)]
      (decode-array string oid opt))))
