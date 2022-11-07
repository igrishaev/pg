(ns pg.conn
  (:require
   [pg.msg :as msg])
  (:import
   java.net.InetSocketAddress
   java.nio.channels.SocketChannel))


(defn initial [^SocketChannel ch]

  (loop [state nil]

    (let [{:as msg :keys [type]}
          (msg/read-message ch)]

      (case type

        :AuthenticationOk
        (recur state)

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
          (assoc state :tx-status tx-status))))))


(comment

  (def -addr
    (new java.net.InetSocketAddress "localhost" 15432))

  (def -ch
    (SocketChannel/open -addr))

  (def -startup
    (msg/make-startup "ivan" "ivan"))

  (.rewind -startup)

  (.write -ch -startup)

  (initial -ch)

  (msg/read-message -ch)

  (msg/read-messages -ch)

  (def -query
    (msg/make-query "select 1 as foo, 2 as bar"))

  (def -query
    (msg/make-query "begin"))

  (.rewind -query)

  (.write -ch -query)







  )
