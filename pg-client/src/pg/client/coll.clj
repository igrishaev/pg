(ns pg.client.coll
  (:import
   java.util.Iterator
   clojure.lang.RT))


(defmacro iter [coll]
  `(RT/iter ~coll))


(defmacro do-n
  {:style/indent 1}
  [[i n] & body]
  `(let [n# ~n]
     (loop [i# 0]
       (when-not (= i# n#)
         (let [~i i#]
           ~@body
           (recur (inc i#)))))))


(defmacro do-seq
  {:style/indent 1}
  [[bind coll] & body]
  `(let [itr# (iter ~coll)]
     (loop [i# 0]
       (when (.hasNext itr#)
         (let [~'&i i#
               ~bind (.next itr#)]
           (do ~@body)
           (recur (inc i#)))))))


(defmacro do-map
  {:style/indent 1}
  [[[k v] m] & body]
  `(let [itr# (iter ~m)]
     (loop [i# 0]
       (when (.hasNext itr#)
         (let [~'&i i#
               item# (.next itr#)
               ~k (key item#)
               ~v (val item#)]
           (do ~@body)
           (recur (inc i#)))))))


(defmacro for-n
  {:style/indent 1}
  [[i n] & body]
  `(let [n# ~n]
     (loop [i# 0
            result# (transient [])]
       (if (= i# n#)
         (persistent! result#)
         (let [~i i#
               item# (do ~@body)]
           (recur (inc i#) (conj! result# item#)))))))


(defmacro for-seq
  {:style/indent 1}
  [[item items] & body]
  `(let [iter# (iter ~items)]
     (loop [i# 0
            result# (transient [])]
       (if (.hasNext iter#)
         (let [~'&i i#
               ~item (.next iter#)
               item# (do ~@body)]
           (recur (inc i#) (conj! result# item#)))
         (persistent! result#)))))


(defn deep-merge
  ([a b]
   (when (or a b)
     (letfn [(merge-entry [m e]
               (let [k  (key e)
                     v' (val e)]
                 (if (contains? m k)
                   (assoc m k (let [v (get m k)]
                                (if (and (map? v) (map? v'))
                                  (deep-merge v v')
                                  v')))
                   (assoc m k v'))))]
       (reduce merge-entry (or a {}) (seq b)))))
  ([a b & more]
   (reduce deep-merge (or a {}) (cons b more))))


(letfn [(->iter [coll]
          (RT/iter coll))
        (has-next? [iter]
          (.hasNext ^Iterator iter))
        (next! [iter]
          (.next ^Iterator iter))]
  (defn map!
    ([f coll]
     (mapv f coll))
    ([f coll & colls]
     (let [iters (map ->iter (cons coll colls))]
       (loop [acc! (transient [])]
         (if (every? has-next? iters)
           (let [item (apply f (map next! iters))]
             (recur (conj! acc! item)))
           (persistent! acc!)))))))


(defmacro for-seqs
  {:style/indent 1}
  [bindings & body]
  (let [ks (take-nth 2 bindings)
        vs (take-nth 2 (rest bindings))]
    `(map! (fn [~@ks] ~@body) ~@vs)))
