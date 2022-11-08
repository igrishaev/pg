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

    (loop [state state]

      (let [{:as msg :keys [type]}
            (msg/read-message ch)]

        (case type

          ;; AuthenticationCleartextPassword
          ;; AuthenticationMD5Password

          :AuthenticationOk
          state

          ;; else
          (throw (ex-info "Unhandled message"
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
        (throw (ex-info "Unhandled message"
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
    (connect {:host "localhost"
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
