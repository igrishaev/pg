(ns pg.error)


(defn error!

  ([message]
   (error! message {} nil))

  ([message data]
   (error! message data nil))

  ([message data cause]
   (throw (ex-info message
                   (assoc data :type ::error)
                   cause))))
