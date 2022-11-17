(ns pg.conn
  (:require
   [pg.bb :as bb]
   [pg.error :as e]
   [pg.handler :as handler]
   [pg.msg :as msg])
  (:import
   java.net.InetSocketAddress
   java.nio.channels.SocketChannel))


(defn write-bb
  [{:as conn :keys [^SocketChannel ch]} bb]
  (let [written
        (.write ch (bb/rewind bb))]
    (if (zero? (bb/remaining bb))
      conn
      (e/error! "Uncomplete write to the channel"
                {:in ::here
                 :bb bb
                 :written written}))))


(defn read-bb [{:keys [^SocketChannel ch]}]
  (msg/read-message ch))


(defmacro with-lock
  [state & body]
  `(locking (:o ~state)
     ~@body))


(def config-defaults
  {:fn-notice-handler handler/notice-handler
   :fn-notification-handler handler/notification-handler})


(defn connect [{:as conn :keys [^String host
                                ^Integer port]}]

  (let [addr
        (new java.net.InetSocketAddress host port)

        ch
        (SocketChannel/open addr)]

    (-> config-defaults
        (merge conn)
        (assoc
         :o (new Object)
         :ch ch
         :addr addr
         :tx-status (atom nil)
         :server-params (atom {})))))


(defn tx-status
  ([conn]
   (-> conn :tx-status deref))

  ([conn value]
   (update conn :tx-status reset! value)))



(defn param
  ([conn pname]
   (-> conn :server-params deref (get pname)))

  ([{:as conn :keys [server-params]} pname value]
   (swap! server-params assoc pname value)
   conn))


(defn server-encoding ^String [conn]
  (or (param conn "server_encoding") "UTF-8"))


(defn client-encoding ^String [conn]
  (or (param conn "client_encoding") "UTF-8"))
