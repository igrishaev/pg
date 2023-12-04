(ns pg.client.ssl
  (:require
   [pg.const :as const])
  (:import
   java.io.BufferedInputStream
   java.net.Socket
   java.util.Map
   javax.net.ssl.SSLContext
   javax.net.ssl.SSLSocket))


(def enabled-protocols
  (into-array String ["TLSv1.2" "TLSv1.1" "TLSv1"]))


(defn wrap-ssl [conn]

  (let [{:keys [^Socket socket
                ^Map state
                config]}
        conn

        {:keys [^String host
                ^Integer port
                ^SSLContext ssl-context]}
        config

        ssl-context
        (or ssl-context
            (SSLContext/getDefault))

        ^SSLSocket ssl-socket
        (-> ssl-context
            (.getSocketFactory)
            (.createSocket socket
                           host
                           port
                           true))

        ssl-out-stream
        (.getOutputStream ssl-socket)

        ssl-in-stream
        (-> ssl-socket
            (.getInputStream)
            (BufferedInputStream. const/IN_STREAM_BUF_SIZE))]

    (doto ssl-socket
      (.setUseClientMode true)
      (.setEnabledProtocols enabled-protocols)
      (.startHandshake))

    (.put state "ssl" true)

    (assoc conn
           :socket ssl-socket
           :in-stream ssl-in-stream
           :out-stream ssl-out-stream)))
