(ns pg.encode.bin.core
  (:require
   [clojure.template :refer [do-template]]))


(defmulti -encode
  (fn [value oid _]
    [(type value) oid]))


(defmacro expand
  {:style/indent 1}
  [type-oid's binding & body]
  `(do-template [Type# oid#]
                (defmethod -encode [Type# oid#]
                  ~binding
                  ~@body)
                ~@type-oid's))


(defmethod -encode :default
  [value oid opt]
  (throw (ex-info "Cannot binary encode a value"
                  {:value value
                   :oid oid
                   :opt opt})))


(defn get-encoding ^String [options]
  (get options :client-encoding "UTF-8"))
