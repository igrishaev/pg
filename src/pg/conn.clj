(ns pg.conn
  (:require
   [pg.const :as const]
   [pg.codec :as codec]
   [pg.scram :as scram]
   [pg.bb :as bb]
   [pg.msg :as msg])
  (:import
   java.net.InetSocketAddress
   java.nio.channels.SocketChannel))


(defn send-bb [^SocketChannel ch bb]
  (.write ch (bb/rewind bb)))


(defn auth-pipeline
  [{:as state :keys [user
                     password
                     database
                     ^SocketChannel ch]}]

  (let [bb
        (msg/make-startup database user const/PROT-VER-14)]

    (send-bb ch bb)

    (loop [state-auth nil]

      (let [{:as msg :keys [type]}
            (msg/read-message ch)]

        (case type

          :AuthenticationOk
          (assoc state :state-auth state-auth)

          :AuthenticationSASLContinue
          (let [{:keys [server-first-message]}
                msg

                state-auth
                (-> state-auth
                    (scram/step2-server-first-message server-first-message)
                    (scram/step3-client-final-message))

                {:keys [client-final-message]}
                state-auth

                bb
                (msg/make-sasl-response client-final-message)]

            (send-bb ch bb)
            (recur state-auth))

          :AuthenticationSASL
          (let [{:keys [sasl-types]}
                msg]

            (cond

              (contains? sasl-types const/SCRAM-SHA-256)
              (let [state-auth
                    (scram/step1-client-first-message user password)

                    {:keys [client-first-message]}
                    state-auth

                    bb
                    (msg/make-sasl-init-response const/SCRAM-SHA-256
                                                 client-first-message)]

                (send-bb ch bb)
                (recur state-auth))

              :else
              (throw (ex-info "Other SCRAM algorithms are not implemented yet"
                              {:sasl-types sasl-types}))))

          :AuthenticationSASLFinal
          (let [{:keys [server-final-message]}
                msg

                state-auth
                (-> state-auth
                    (scram/step4-server-final-message server-final-message)
                    (scram/step5-verify-server-signatures))]
            (recur state-auth))

          :AuthenticationCleartextPassword
          (let [bb (msg/make-clear-text-password password)]
            (send-bb ch bb)
            (recur nil))

          :AuthenticationMD5Password
          (let [{:keys [salt]} msg
                bb (msg/make-md5-password user password salt)]
            (send-bb ch bb)
            (recur nil))

          :ErrorResponse
          (let [{:keys [errors]} msg]
            (throw (ex-info "Authentication failed"
                            {:errors errors})))

          ;; else
          (throw (ex-info "Unhandled message in the auth pipeline"
                          {:msg msg})))))))


(defn init-pipeline
  [{:as state :keys [^SocketChannel ch]}]

  (loop [state state]

    (let [{:as msg :keys [type]}
          (msg/read-message ch)]

      (case type

        :ParameterStatus
        (let [{:keys [param value]} msg]
          (recur (assoc-in state [:params param] value)))

        :BackendKeyData
        (let [{:keys [pid secret-key]} msg]
          (recur (assoc state
                        :pid pid
                        :secret-key secret-key)))

        :ReadyForQuery
        (let [{:keys [tx-status]} msg]
          (case tx-status
            \E
            (throw (ex-info "Transaction is in the error state"
                            {:msg msg}))
            ;; else
            (assoc state :tx-status tx-status)))

        ;; else
        (throw (ex-info "Unhandled message in the init pipeline"
                        {:msg msg}))))))


(defn connect [{:as state :keys [^String host
                                 ^Integer port]}]

  (let [addr
        (new java.net.InetSocketAddress host port)

        ch
        (SocketChannel/open addr)]

    (-> state
        (assoc :ch ch
               :addr addr)
        (auth-pipeline)
        (init-pipeline))))


(defn query
  [{:as state :keys [ch]} sql]
  (let [bb (msg/make-query sql)]
    (send-bb ch bb)
    )



  )


(comment

  (def -state
    (connect {:host "127.0.0.1"
              :port 15432
              :user "ivan"
              :database "ivan"
              :password "secret"}))

  (def -query
    (msg/make-query "select 1 as foo, 2 as bar"))

  (def -query
    (msg/make-query "begin"))

  (.rewind -query)

  (.write -ch -query)







  )
