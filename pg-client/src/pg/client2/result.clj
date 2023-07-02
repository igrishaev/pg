(ns pg.client2.result
  (:require
   [pg.client2.conn :as conn]))


(defn make-result []
  {})


(defn handle-ReadyForQuery
  [conn result {:keys [tx-status]}]
  (conn/set-tx-status conn tx-status)
  result)


(defn handle-BackendKeyData
  [conn result {:keys [pid secret-key]}]
  (conn/set-pid conn pid)
  (conn/set-secret-key conn secret-key)
  result)


(defn handle [conn result {:as message :keys [msg]}]

  (case msg

    :ReadyForQuery
    (handle-ReadyForQuery conn result message)

    :BackendKeyData
    (handle-BackendKeyData conn result message)

    ;; else

    result))


(defn interact [conn until]

  (loop [result (make-result)]

    (let [{:as message :keys [msg]}
          (conn/read-message conn)]

      (let [result
            (handle conn result message)]

        (if (contains? until msg)
          result
          (recur result))))))
