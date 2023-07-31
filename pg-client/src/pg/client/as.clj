(ns pg.client.as
  (:require
   [pg.client.func :as func])
  (:import
   java.util.ArrayList
   java.util.HashMap))


(def default
  {:fn-init #(transient [])
   :fn-reduce conj!
   :fn-finalize persistent!})


(def java
  {:fn-keyval func/zipmap-java
   :fn-init #(new ArrayList)
   :fn-reduce (fn [^ArrayList array-list row]
                (doto array-list (.add row)))})


(def kebab-keys
  (assoc default
         :fn-column func/kebab-keyword))


(def matrix
  {:fn-unify func/unify-none
   :fn-keyval func/vals-only
   :fn-init #(transient [])
   :fn-reduce conj!
   :fn-finalize persistent!})


(defn index-by [f]
  {:fn-init #(transient {})
   :fn-reduce (fn [acc! row]
                (assoc! acc! (f row) row))
   :fn-finalize persistent!})


(defn group-by [f]
  {:fn-init hash-map
   :fn-reduce (let [-conj (fnil conj [])]
                (fn [acc row]
                  (update acc (f row) -conj row)))})


(defn kv [fk fv]
  {:fn-init #(transient {})
   :fn-reduce (fn [acc! row]
                (assoc! acc! (fk row) (fv row)))
   :fn-finalize persistent!})
