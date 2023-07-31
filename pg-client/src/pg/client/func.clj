(ns pg.client.func
  (:require
   [clojure.string :as str])
  (:import
   clojure.lang.Keyword
   java.util.Map
   java.util.List
   java.util.HashMap))


(defn kebab-keyword ^Keyword [^String column]
  (-> column (str/replace #"_" "-") keyword))


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


(defn unify-idx ^List [^List Keys]
  (let [len (count Keys)
        inc' (fnil inc 0)]
    (loop [i 0
           k-n {}
           res []]
      (if (= i len)
        res
        (let [k (.get Keys i)]
          (if-let [n (get k-n k)]
            (recur (inc i)
                   (update k-n k inc')
                   (conj res (format "%s_%s" k n)))
            (recur (inc i)
                   (update k-n k inc')
                   (conj res k))))))))


(defn unify-none [the-keys]
  the-keys)


(defn vals-only [_ the-vals]
  the-vals)
