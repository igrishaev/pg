(ns pg.pipeline
  (:require
   [pg.error :as e]
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


(defn data [conn]
  (loop []
    (let [msg
          (conn/read-bb conn)]
      (println msg)
      (recur))))
