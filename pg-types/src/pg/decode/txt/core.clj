(ns pg.decode.txt.core
  (:require
   [clojure.template :refer [do-template]]))


(defmulti -decode
  (fn [^String _string oid _opt]
    oid))


(defmethod -decode :default
  [string _ _]
  string)


(defmacro expand
  {:style/indent 1}
  [oid's binding & body]
  `(do-template [oid#]
                (defmethod -decode oid#
                  ~binding
                  ~@body)
                ~@oid's))
