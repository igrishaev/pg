(ns pg.decode.bin.core
  (:require
   [clojure.template :refer [do-template]]))


(defn get-server-encoding ^String [opt]
  (get opt :server-encoding "UTF-8"))


(defmulti -decode
  (fn [^bytes _buf oid _opt]
    oid))


(defmethod -decode :default
  [buf _ _]
  buf)


(defmacro expand
  {:style/indent 1}
  [oid's binding & body]
  `(do-template [oid#]
                (defmethod -decode oid#
                  ~binding
                  ~@body)
                ~@oid's))
