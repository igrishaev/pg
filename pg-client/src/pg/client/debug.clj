(ns pg.client.debug)


(defmacro debug-message [message prefix]
  (when true #_(System/getenv "PG_DEBUG_MESSAGE")
    `(println ~prefix ~message)))
