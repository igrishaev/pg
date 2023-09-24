(ns pg.client.ssl
  (:import
   java.net.Socket
   java.util.Map
   javax.net.ssl.SSLContext
   javax.net.ssl.SSLSocket)
  (:require
   [less.awful.ssl :as ssl]))


(defn is-ssl-needed? [conn]
  (-> conn :config :ssl not-empty))


(defn wrap-ssl [conn]

  (let [{:keys [^Socket socket
                ^Map state
                config]}
        conn

        {:keys [^String host
                ^Integer port]
         ssl-opt :ssl}
        config

        {:keys [key-file
                cert-file
                ca-cert-file]}
        ssl-opt

        ^SSLContext ssl-context
        (if ca-cert-file
          (ssl/ssl-context key-file cert-file ca-cert-file)
          (ssl/ssl-context key-file cert-file))

        socket-factory
        (.getSocketFactory ssl-context)

        ^SSLSocket ssl-socket
        (.createSocket socket-factory
                       socket
                       host
                       port
                       true)

        ssl-in-stream
        (.getOutputStream ssl-socket)

        ssl-out-stream
        (.getInputStream ssl-socket)]

    (doto ssl-socket
      (.setUseClientMode true)
      (.setEnabledProtocols ssl/enabled-protocols)
      (.startHandshake))

    (.put state "ssl" true)

    (assoc conn
           :socket ssl-socket
           :in-stream ssl-in-stream
           :out-stream ssl-out-stream)))


(defn maybe-wrap-ssl [conn]
  (if (is-ssl-needed? conn)
    (wrap-ssl conn)
    conn))
