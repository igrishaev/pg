(ns pg.decode.bin
  (:require
   [clojure.template :refer [do-template]]
   [pg.bytes :as bytes]
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


;;
;; Numbers
;;

(expand [oid/int2]
  [buf _ _]
  (bytes/bytes->int16 buf))


(expand [oid/int4]
  [buf _ _]
  (bytes/bytes->int32 buf))
