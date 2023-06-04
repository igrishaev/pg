(ns pg.encode.txt
  (:import
   clojure.lang.MultiFn))


(defmulti -encode
  (fn [value oid _]
    [(type value) oid]))


(defmethod -encode :default
  [value oid opt]
  (throw (ex-info "Cannot binary encode a value"
                  {:value value
                   :oid oid
                   :opt opt})))


(defn set-default [Type oid]
  (let [method
        (.getMethod ^MultiFn -encode [Type oid])

        default
        (.getMethod ^MultiFn -encode :default)]

    (if (or (nil? method) (= method default))
      (throw (ex-info (format "There is no a method with [%s %s] dispatch value." Type oid)
                      {:type type
                       :oid oid}))
      (.addMethod ^MultiFn -encode [Type nil] method))))


;;
;; API
;;

(defn encode
  (^String [value]
   (-encode value nil nil))

  (^String [value oid]
   (-encode value oid nil))

  (^String [value oid opt]
   (-encode value oid opt)))
