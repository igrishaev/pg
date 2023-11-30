(ns pg.client.debug)


(def DEBUG? false
  #_
  (some?
   (or (System/getenv "PG_DEBUG")
       (System/getProperty "pg.debug"))))


(when DEBUG?
  (defmethod print-method (type (byte-array []))
    [b writer]
    (print-method (vec b) writer)))


(defmacro debug-message [message prefix]
  (when DEBUG?
    `(println ~prefix ~message)))
