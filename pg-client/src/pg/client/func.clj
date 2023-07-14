(ns pg.client.func
  (:import
   java.util.Map
   java.util.List
   java.util.HashMap))


(defn zipmap-java ^Map [^List the-keys ^List the-vals]
  (let [len
        (.size the-keys)
        res
        (new HashMap)]
    (loop [i 0]
      (if (= i len)
        res
        (let [the-key
              (.get the-keys i)
              the-val
              (.get the-vals i)]
          (.put res the-key the-val)
          (recur (inc i)))))))


(defn reduce-list
  [^List acc row]
  (doto acc
    (.add acc row)))
