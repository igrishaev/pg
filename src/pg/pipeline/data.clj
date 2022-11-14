(ns pg.pipeline.data
  (:require
   [pg.error :as e]
   [pg.types :as types]
   [pg.conn :as conn]
   [pg.codec :as codec]
   [pg.const :as const]
   [pg.msg :as msg]))


(defn ex? [e]
  (instance? Throwable e))


(defn decode-row
  [DataRow RowDescription enc]

  (let [{:keys [fields
                field-count]}
        RowDescription

        {:keys [columns]}
        DataRow]

    (loop [i 0
           acc! (transient {})]

      (if (= i field-count)
        (persistent! acc!)

        (let [column
              (get columns i)

              field
              (get fields i)

              cname
              (-> field
                  :name
                  (codec/bytes->str enc)
                  keyword)

              cvalue
              (types/parse-column column field enc)]

          (recur (inc i)
                 (assoc! acc! cname cvalue)))))))


(defn process-ready-message [conn state msg]

  (let [enc
        (conn/server-encoding conn)

        {:keys [tx-status]}
        msg

        {:keys [e
                Rows!
                ErrorResponse]}
        state]

    (cond

      e
      (e/error! "Unhandled exception"
                {:state state}
                e)

      (= tx-status const/TX-ERROR)
      (e/error! "Transaction is in the error state"
                {:msg msg})

      ErrorResponse
      (let [{:keys [errors]}
            ErrorResponse

            message
            (with-out-str
              (println "ErrorResponse during the data pipeline")
              (doseq [{:keys [label bytes]} errors]
                (println " -" label (codec/bytes->str bytes enc))))]

        (e/error! message))

      Rows!
      (persistent! Rows!))))


(defn process-another-message
  [conn state {:as msg :keys [type]}]

  (let [enc
        (conn/server-encoding conn)]

    (case type

      :CloseComplete
      state

      :ParseComplete
      state

      :ErrorResponse
      (assoc state :ErrorResponse msg)

      :RowDescription
      (assoc state
             :Rows! (transient [])
             :RowDescription msg)

      :DataRow
      (let [{:keys [RowDescription]}
            state

            Row
            (decode-row msg RowDescription enc)]

        (update state :Rows! conj! Row))

      :CommandComplete
      (assoc state :CommandComplete msg))))


(defn pipeline [conn]

  (let [enc
        (conn/server-encoding conn)]

    (loop [state nil]

      (let [{:as msg :keys [type]}
            (conn/read-bb conn)]

        (case type

          :ReadyForQuery
          (process-ready-message conn state msg)

          ;; else
          (let [[state-next e]
                (try
                  [(process-another-message conn state msg) nil]
                  (catch Throwable e
                    [nil e]))]

            (if e
              (recur (assoc state :e e))
              (recur state-next))))))))
