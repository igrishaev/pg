(ns pg.conn
  (:require
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

    (loop []

      (let [{:as msg :keys [type]}
            (msg/read-message ch)]

        (case type

          :AuthenticationOk
          state

          ; :AuthenticationSASL

          :AuthenticationCleartextPassword
          (let [bb (msg/make-clear-text-password password)]
            (send-bb ch bb)
            (recur))

          :AuthenticationMD5Password
          (let [{:keys [salt]} msg
                bb (msg/make-md5-password user password salt)]
            (send-bb ch bb)
            (recur))

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
              :password "ivan"}))

  (def -query
    (msg/make-query "select 1 as foo, 2 as bar"))

  (def -query
    (msg/make-query "begin"))

  (.rewind -query)

  (.write -ch -query)







  )
