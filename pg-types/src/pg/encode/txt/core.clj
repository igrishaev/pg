(ns pg.encode.txt.core
  (:require
   [clojure.template :refer [do-template]]))


(defmulti -encode
  (fn [value oid _]
    [(type value) oid]))


(defmethod -encode :default
  [value oid opt]
  (throw (ex-info "Cannot text-encode a value"
                  {:value value
                   :oid oid
                   :opt opt})))


(defmacro expand
  {:style/indent 1}
  [type-oid's binding & body]
  `(do-template [Type# oid#]
                (defmethod -encode [Type# oid#]
                  ~binding
                  ~@body)
                ~@type-oid's))
