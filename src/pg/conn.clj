(ns pg.conn
  (:require
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

  (let [bb (msg/make-startup database user)]

    (send-bb ch bb)

    (loop [state-sasl nil]

      (let [{:as msg :keys [type]}
            (msg/read-message ch)]

        (println msg)

        (case type

          :AuthenticationOk
          state

          :AuthenticationSASLContinue
          (let [{:keys [message]}
                msg

                server-first-message
                (codec/bytes->str message)

                state-sasl
                (-> state-sasl
                    (scram/step2-server-first-message server-first-message)
                    (scram/step3-client-final-message))

                {:keys [client-final-message]}
                state-sasl

                _ (println server-first-message)
                _ (println client-final-message)

                bb
                (msg/make-sasl-response client-final-message)]

            (send-bb ch bb)
            (recur state-sasl))

          :AuthenticationSASL
          (let [{:keys [auth-types]}
                msg

                state-sasl
                (scram/step1-client-first-message user password)

                {:keys [client-first-message]}
                state-sasl

                bb
                (msg/make-sasl-init-response "SCRAM-SHA-256"
                                             client-first-message)]

            (println client-first-message)

            (send-bb ch bb)
            (recur state-sasl))

          ;; server-final-message

          ;; verifier        = "v=" base64
          ;; base-64 encoded ServerSignature.
          ;; "v=dinCviSchyXpv0W3JPXaT3QYUotxzTWPL8Mw103bRbM="

          :AuthenticationSASLFinal
          (let [{:keys [message]} msg]
            )



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
