(ns pg.client.debug)


(when (System/getenv "PG_DEBUG")
  (defmethod print-method (type (byte-array []))
    [b writer]
    (print-method (vec b) writer)))


(defmacro debug-message [message prefix]
  (when (System/getenv "PG_DEBUG")
    `(println ~prefix ~message)))
