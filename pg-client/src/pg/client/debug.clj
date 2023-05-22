(ns pg.client.debug)


(defmacro debug-message [message prefix]
  (when (System/getenv "PG_DEBUG")
    `(println ~prefix ~message)))
