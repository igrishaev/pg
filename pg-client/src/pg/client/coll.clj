(ns pg.client.coll)


(defmacro do-n
  {:style/indent 1}
  [[i n] & body]
  `(let [n# ~n]
     (loop [i# 0]
       (when-not (= i# n#)
         (let [~i i#]
           ~@body
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


(defmacro do-list
  {:style/indent 1}
  [[i item items] & body]
  `(let [items# ~items
         len# (count items#)]
     (loop [i# 0]
       (when-not (= i# len#)
         (let [~item (.get items# i#)
               ~i i#]
           ~@body
           (recur (inc i#)))))))


(defmacro for-list
  {:style/indent 1}
  [[i item items] & body]
  `(let [items# ~items
         len# (count items#)]
     (loop [i# 0
            result# (transient [])]
       (if (= i# len#)
         (persistent! result#)
         (let [~item (.get items# i#)
               ~i i#
               item# (do ~@body)]
           (recur (inc i#) (conj! result# item#)))))))


(defmacro do-map
  {:style/indent 1}
  [[k v mapping] & body]
  `(run! (fn [entry#]
           (let [~k (key entry#)
                 ~v (val entry#)]
             ~@body))
         ~mapping))


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
