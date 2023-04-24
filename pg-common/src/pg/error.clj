(ns pg.error)


(def ^:dynamic *context* {})


(defmacro with-context [mapping & body]
  `(binding [*context* (merge *context* ~mapping)]
     ~@body))


(defn error!
  ([message]
   (throw (ex-info message *context*)))

  ([template & args]
   (error! (apply format template args))))
