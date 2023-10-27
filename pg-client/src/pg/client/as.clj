(ns pg.client.as
  (:require
   [pg.client.func :as func])
  (:refer-clojure :exclude [first group-by])
  (:import
   java.util.ArrayList
   clojure.lang.PersistentVector))


;;
;; Reducers
;;

(def default
  {:fn-init #(transient [])
   :fn-reduce conj!
   :fn-finalize persistent!})


(def first
  "
  Return the first value skipping all the rest rows.
  "
  {:fn-init (fn [] [])
   :fn-reduce (fn [^PersistentVector acc x]
                (if (.isEmpty acc)
                  (conj acc x)
                  acc))
   :fn-finalize clojure.core/first})


(def java
  "
  Return an ArrayList of HashMaps.
  "
  {:fn-keyval func/zipmap-java
   :fn-init #(new ArrayList)
   :fn-reduce (fn [^ArrayList array-list row]
                (doto array-list (.add row)))})


(def kebab-keys
  "
  Return a vector of Clojure maps where the keys
  transformed to `:kebab-case-keywords`.
  "
  {:fn-column func/kebab-keyword})


(def matrix
  "
  Return a vector of vectors of values.
  "
  {:fn-unify func/unify-none
   :fn-keyval func/vals-only
   :fn-init #(transient [])
   :fn-reduce conj!
   :fn-finalize persistent!})


(defn index-by
  "
  Return a Clojure map of {(f row) => row}. The function
  can be a keyword, for example for `:id`, you'll get a map
  like `{1 {:id 1, name '...'}, 2 {:id 2, name '...'}}`.
  "
  [f]
  {:fn-init #(transient {})
   :fn-reduce (fn [acc! row]
                (assoc! acc! (f row) row))
   :fn-finalize persistent!})


(defn group-by
  "
  Return a map of {(f row) => [row1, row2, ...]}. The function
  can be a keyword, for example for `:name`, you'll get a map
  like `{'ivan' [{:id 1, ...} {:id 2, ...}]}`.
  "
  [f]
  {:fn-init hash-map
   :fn-reduce (let [-conj (fnil conj [])]
                (fn [acc row]
                  (update acc (f row) -conj row)))})


(defn kv
  "
  Return a map of {(fk row) => (fv row)}.
  The `fk` is a key function, and `fv` is a value function.
  Both functions accept the same row.
  "
  [fk fv]
  {:fn-init #(transient {})
   :fn-reduce (fn [acc! row]
                (assoc! acc! (fk row) (fv row)))
   :fn-finalize persistent!})


(defn run
  "
  Run the `f` function for each row in the result.
  Return a number of rows processed. For side effects
  only.
  "
  [f]
  {:fn-init (constantly 0)
   :fn-reduce (fn [n row]
                (f row)
                (inc n))
   :fn-finalize identity})


(defn fold
  "
  Reduce the rows using the init value (an accumulator)
  and a function that accepts the current accumulator and
  the next row. The function should return the new accumulator.
  "
  [init f]
  {:fn-init (constantly init)
   :fn-reduce f
   :fn-finalize identity})
