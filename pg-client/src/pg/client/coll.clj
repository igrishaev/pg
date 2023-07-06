(ns pg.client.coll)


;; TODO: fix/drop this
(defmacro doN
  {:style/indent 1}
  [[bind n] & body]
  `(let [n# ~n]
     (loop [i# 0
            result# (transient [])]
       (if (= i# n#)
         (persistent! result#)
         (let [~bind i#
               item# (do ~@body)]
           (recur (inc i#) (conj! result# item#)))))))


(defmacro do-n
  {:style/indent 1}
  [[bind n] & body]
  `(let [n# ~n]
     (loop [i# 0]
       (when-not (= i# n#)
         (let [~bind i#]
           (do ~@body)
           (recur (inc i#)))))))


(defmacro do-list
  {:style/indent 1}
  [[bind items] & body]
  `(let [items# ~items
         len# (count items#)]
     (loop [i# 0]
       (when-not (= i# len#)
         (let [~bind (get items# i#)]
           (do ~@body)
           (recur (inc i#)))))))



;; TODO: fix/drop this
(defmacro forvec
  {:style/indent 1}
  [[bind items] & body]
  `(let [items# ~items
         len# (count items#)]
     (loop [i# 0
            result# (transient [])]
       (if (= i# len#)
         (persistent! result#)
         (let [~bind (get items# i#)
               item# (do ~@body)]
           (recur (inc i#) (conj! result# item#)))))))
