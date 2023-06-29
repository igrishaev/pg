(ns pg.client2.result
  (:require
   [pg.client2.conn :as conn]))


(defn create [connection]
  )


(defn handle [result message]
  )



(defn make-result [conn]
  )


(defn interact [conn until]
  (loop [result (make-result conn)]
    (let [msg (conn/read-message conn)])


    )






  )
