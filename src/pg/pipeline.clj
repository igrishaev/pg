(ns pg.pipeline
  (:require
   [pg.error :as e]
   [pg.types :as types]
   [pg.conn :as conn]
   [pg.codec :as codec]
   [pg.const :as const]
   [pg.auth.scram-sha-256 :as sha-256]
   [pg.msg :as msg]))


(defn auth
  [{:as conn :keys [user
                    password
                    database]}]

  (let [bb
        (msg/make-startup database user const/PROT-VER-14)]

    (conn/write-bb conn bb)

    (loop [state nil]

      (let [{:as msg :keys [type]}
            (conn/read-bb conn)]

        (case type

          ;; :AuthenticationKerberosV5
          ;; :AuthenticationSCMCredential
          ;; :AuthenticationGSS
          ;; :AuthenticationGSSContinue
          ;; :AuthenticationSSPI

          :AuthenticationOk
          (assoc conn :auth state)

          :AuthenticationSASLContinue
          (let [{:keys [server-first-message]}
                msg

                state
                (-> state
                    (sha-256/step2-server-first-message server-first-message)
                    (sha-256/step3-client-final-message))

                {:keys [client-final-message]}
                state

                bb
                (msg/make-sasl-response client-final-message)]

            (conn/write-bb conn bb)
            (recur state))

          :AuthenticationSASL
          (let [{:keys [sasl-types]}
                msg]

            (cond

              (contains? sasl-types const/SCRAM-SHA-256)
              (let [state
                    (sha-256/step1-client-first-message user password)

                    {:keys [client-first-message]}
                    state

                    bb
                    (msg/make-sasl-init-response const/SCRAM-SHA-256
                                                 client-first-message)]

                (conn/write-bb conn bb)
                (recur state))

              :else
              (e/error! "No other SCRAM algorithms have been implemented yet"
                        {:sasl-types sasl-types})))

          :AuthenticationSASLFinal
          (let [{:keys [server-final-message]}
                msg

                state
                (-> state
                    (sha-256/step4-server-final-message server-final-message)
                    (sha-256/step5-verify-server-signatures))]
            (recur state))

          :AuthenticationCleartextPassword
          (let [bb (msg/make-clear-text-password password)]
            (conn/write-bb conn bb)
            (recur nil))

          :AuthenticationMD5Password
          (let [{:keys [salt]} msg
                bb (msg/make-md5-password user password salt)]
            (conn/write-bb conn bb)
            (recur nil))

          :ErrorResponse
          (let [{:keys [errors]} msg]
            (e/error! "ErrorResponse during the Authentication pipeline"
                      {:msg msg}))

          ;; else
          (e/error! "Unhandled message during the Authentication pipeline"
                    {:msg msg}))))))



(defn init [conn]

  (loop [conn conn]

    (let [{:as msg :keys [type]}
          (conn/read-bb conn)]

      (case type

        :ParameterStatus
        (let [{:keys [param value]} msg]
          (recur (assoc-in conn [:server-params param] value)))

        :BackendKeyData
        (let [{:keys [pid secret-key]} msg]
          (recur (assoc conn
                        :pid pid
                        :secret-key secret-key)))

        :ReadyForQuery
        (let [{:keys [tx-status]} msg]
          (case tx-status
            \E
            (e/error! "Transaction is in the error state"
                      {:msg msg})

            ;; else
            (assoc conn :tx-status tx-status)))

        ;; else
        (e/error! "Unhandled message in the initialization pipeline"
                  {:msg msg})))))


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


(defn data [conn]

  (let [enc
        (conn/server-encoding conn)]

    (loop [state nil]

      (let [{:as msg :keys [type]}
            (conn/read-bb conn)]

        (println msg)

        (case type

          :CloseComplete
          (recur state)

          :ParseComplete
          (recur state)

          :ErrorResponse
          (recur (assoc state :ErrorResponse msg))

          :RowDescription
          (recur
           (assoc state
                  :Rows! (transient [])
                  :RowDescription msg))

          :DataRow
          (let [{:keys [RowDescription]}
                state

                Row
                (decode-row msg RowDescription enc)]

            (recur
             (update state :Rows! conj! Row)))

          :CommandComplete
          (recur (assoc state :CommandComplete msg))

          :ReadyForQuery
          (let [{:keys [tx-status]}
                msg

                {:keys [Rows!
                        ErrorResponse]}
                state]

            (cond

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
                        (println " -"label (codec/bytes->str bytes enc))))]
                (e/error! message))

              Rows!
              (persistent! Rows!))))))))
