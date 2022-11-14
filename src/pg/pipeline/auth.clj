(ns pg.pipeline.auth
  (:require
   [pg.error :as e]
   [pg.conn :as conn]
   [pg.codec :as codec]
   [pg.const :as const]
   [pg.auth.md5 :as md5]
   [pg.auth.scram-sha-256 :as sha-256]
   [pg.msg :as msg]))


(defn pipeline
  [{:as conn :keys [user
                    password
                    database]}]

  (let [bb
        (msg/make-startup database user const/PROT-VER-14)]

    (conn/write-bb conn bb)

    (loop [conn conn
           auth nil]

      (let [{:as msg :keys [type]}
            (conn/read-bb conn)]

        (case type

          ;; :AuthenticationKerberosV5
          ;; :AuthenticationSCMCredential
          ;; :AuthenticationGSS
          ;; :AuthenticationGSSContinue
          ;; :AuthenticationSSPI

          :AuthenticationOk
          (recur (assoc conn :auth auth) auth)

          :AuthenticationSASLContinue
          (let [{:keys [server-first-message]}
                msg

                {:as auth :keys [client-final-message]}
                (-> auth
                    (sha-256/step2-server-first-message server-first-message)
                    (sha-256/step3-client-final-message))

                bb
                (msg/make-sasl-response client-final-message)]

            (conn/write-bb conn bb)
            (recur conn auth))

          :AuthenticationSASL
          (let [{:keys [sasl-types]}
                msg]

            (cond

              (contains? sasl-types const/SCRAM-SHA-256)
              (let [{:as auth :keys [client-first-message]}
                    (sha-256/step1-client-first-message user password)

                    bb
                    (msg/make-sasl-init-response const/SCRAM-SHA-256
                                                 client-first-message)]

                (conn/write-bb conn bb)
                (recur conn auth))

              :else
              (e/error! "No other SCRAM algorithms have been implemented yet"
                        {:sasl-types sasl-types})))

          :AuthenticationSASLFinal
          (let [{:keys [server-final-message]}
                msg

                auth
                (-> auth
                    (sha-256/step4-server-final-message server-final-message)
                    (sha-256/step5-verify-server-signatures))]
            (recur conn auth))

          :AuthenticationCleartextPassword
          (let [bb (msg/make-clear-text-password password)]
            (conn/write-bb conn bb)
            (recur conn auth))

          :AuthenticationMD5Password
          (let [{:keys [salt]}
                msg

                hashed-pass
                (md5/hash-password user password salt)

                bb
                (msg/make-md5-password
                 (codec/str->bytes hashed-pass))]

            (conn/write-bb conn bb)
            (recur conn auth))

          :ErrorResponse
          (let [{:keys [errors]} msg]
            (e/error! "ErrorResponse during the Authentication pipeline"
                      {:msg msg}))

          :ParameterStatus
          (let [{:keys [param value]} msg]
            (recur (assoc-in conn [:server-params param] value)
                   auth))

          :BackendKeyData
          (let [{:keys [pid secret-key]} msg]
            (recur (assoc conn
                          :pid pid
                          :secret-key secret-key)
                   auth))

          :ReadyForQuery
          (let [{:keys [tx-status]} msg]
            (cond
              (= tx-status const/TX-ERROR)
              (e/error! "Transaction is in the error state"
                        {:msg msg})
              :else
              conn))

          ;; else
          (e/error! "Unhandled message during the Authentication pipeline"
                    {:msg msg}))))))
