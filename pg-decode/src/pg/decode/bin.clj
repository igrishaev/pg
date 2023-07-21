(ns pg.decode.bin
  (:require
   [clojure.template :refer [do-template]]
   [pg.oid :as oid]))


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


;;
;; API
;;

(defn decode

  ([^bytes buf oid]
   (-decode buf oid nil))

  ([^bytes buf oid opt]
   (-decode buf oid opt)))
