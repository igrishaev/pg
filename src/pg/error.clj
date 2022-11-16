(ns pg.error)

(defn error

  ([message]
   (error message {} nil))

  ([message data]
   (error message data nil))

  ([message data cause]
   (ex-info message
            (assoc data :type ::error)
            cause)))


(defn error!

  ([message]
   (throw (error message)))

  ([message data]
   (throw (error message data)))

  ([message data cause]
   (throw (error message data cause))))


(defmacro with-pcall [& body]
  `(try
     [(do ~@body) nil]
     (catch Throwable e#
       [nil e#])))
