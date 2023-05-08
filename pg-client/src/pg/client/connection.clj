(ns pg.client.connection
  (:require
   [pg.client.message :as message]
   [pg.error :as e]
   [pg.client.parse :as parse]
   [pg.client.compose :as compose]
   [pg.client.bb :as bb])
  (:import
   java.nio.ByteBuffer
   java.io.Closeable
   java.util.Map
   java.util.HashMap
   java.net.InetSocketAddress
   java.nio.channels.SocketChannel))


(defn read-bb [^SocketChannel ch ^ByteBuffer bb]
  (while (not (zero? (bb/remaining bb)))
    (.read ch bb)))


(defprotocol IConnection

  (set-pid [this pid])

  (get-pid [this])

  (set-secret-key [this secret-key])

  (get-secret-key [this])

  (set-tx-status [this tx-status])

  (get-tx-status [this])

  (set-parameter [this param value])

  (get-parameter [this param])

  (read-message [this])

  (message-seq [this])

  (send-message [this bb])

  (authenticate [this]))


(deftype Connection
    [^Map -config
     ^InetSocketAddress -addr
     ^SocketChannel -ch
     ^Map -params]

  IConnection

  (set-parameter [this param value]
    (.put -params param value))

  (get-parameter [this param]
    (.get -params param))

  (send-message [this bb]

    (let [written (.write -ch (bb/rewind bb))
          remaining (bb/remaining bb)]
      (when-not (zero? remaining)
        (e/error! "Incomplete record to the channel, written: %s, remaining: %s"
                  written remaining))))

  (read-message [this]

    (let [bb-head
          (bb/allocate 5)]

      (read-bb -ch bb-head)
      (bb/rewind bb-head)

      (let [tag
            (char (bb/read-byte bb-head))

            len
            (- (bb/read-int32 bb-head) 4)

            bb-body
            (bb/allocate len)]

        (read-bb -ch bb-body)
        (bb/rewind bb-body)

        (parse/-parse tag bb-body))))

  (message-seq [this]
    (lazy-seq
     (let [message (read-message this)]
       (if (message/ready-for-query? message)
         [message]
         (cons message (message-seq this))))))

  (authenticate [this]

    (let [{:keys [database user]}
          -config

          bb
          (compose/startup database user)]

      (send-message this bb)))

  Closeable

  (close [this]
    (.close -ch)))


(defn connect [{:as config
                :keys [^String host
                       ^Integer port]}]

  (let [addr
        (new java.net.InetSocketAddress host port)

        ch
        (SocketChannel/open addr)

        params
        (new HashMap)]

    (new Connection
         config
         addr
         ch
         params)))


#_
(comment

  (def -c (connect {:host "127.0.0.1"
                    :port 15432
                    :user "ivan"
                    :password "ivan"
                    :database "ivan"}))

  (authenticate -c)

  (def -m (read-message -c))

  (vec (message-seq -c))

  (def -q (compose/query "select 1 as foo; select 2 as bar"))

  (send-message -c -q)

  (run! println (message-seq -c))

  )
