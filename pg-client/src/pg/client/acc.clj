(ns pg.client.acc
  (:require
   [pg.client.func :as func])
  (:import
   java.util.ArrayList
   java.util.HashMap))


(def as-default
  {:fn-init #(transient [])
   :fn-reduce (fn [acc! row]
                (conj! acc! row))
   :fn-finalize persistent!})


(def as-java
  {:fn-keyval func/zipmap-java
   :fn-init #(new ArrayList)
   :fn-reduce (fn [^ArrayList array-list row]
                (doto array-list (.add row)))})


(def as-matrix
  {:fn-unify func/unify-none
   :fn-keyval func/vals-only
   :fn-init #(transient [])
   :fn-reduce (fn [acc! row]
                (conj! acc! row))
   :fn-finalize persistent!})


(defn as-index-by [f]
  {:fn-init #(transient {})
   :fn-reduce (fn [acc! row]
                (assoc! acc! (f row) row))
   :fn-finalize persistent!})


(defn as-group-by [f]
  {:fn-init hash-map
   :fn-reduce (let [-conj (fnil conj [])]
                (fn [acc row]
                  (update acc (f row) -conj row)))})


(defn as-kv [fk fv]
  {:fn-init #(transient {})
   :fn-reduce (fn [acc! row]
                (assoc! acc! (fk row) (fv row)))
   :fn-finalize persistent!})
