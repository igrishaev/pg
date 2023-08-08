

(defn ->csv [chunk]
  (with-out-str
    (doseq [[a b c] chunk]
      (println a \, b \, c))))

(defn ->input-stream ^InputStream [^String text]
  (-> text
      (.getBytes "UTF-8")
      clojure.java.io/input-stream))

(-> chunk
    ->csv
    ->input-stream)



{:tag :ParameterStatus, :param "application_name", :value ""}
{:tag :ParameterStatus, :param "client_encoding", :value "UTF8"}
{:tag :ParameterStatus, :param "DateStyle", :value "ISO, MDY"}
{:tag :ParameterStatus, :param "default_transaction_read_only", :value "off"}
{:tag :ParameterStatus, :param "in_hot_standby", :value "off"}
{:tag :ParameterStatus, :param "integer_datetimes", :value "on"}
{:tag :ParameterStatus, :param "IntervalStyle", :value "postgres"}
{:tag :ParameterStatus, :param "is_superuser", :value "on"}
{:tag :ParameterStatus, :param "server_encoding", :value "UTF8"}
{:tag :ParameterStatus, :param "server_version", :value "14.6"}
{:tag :ParameterStatus, :param "session_authorization", :value "ivan"}
{:tag :ParameterStatus, :param "standard_conforming_strings", :value "on"}
{:tag :ParameterStatus, :param "TimeZone", :value "Europe/Moscow"}

(.withZone (ZoneOffset/systemDefault))


-      -c ssl=on
-      -c ssl_cert_file=/var/lib/postgresql/server.crt
-      -c ssl_key_file=/var/lib/postgresql/server.key

-      - "./certs/server-cert.pem:/var/lib/postgresql/server.crt:ro"
-      - "./certs/server-key.pem:/var/lib/postgresql/server.key:ro"


(defn batch [conn]

  (let [q1 "select $1::integer as one"
        q2 "select $1::text as msg"

        s1 (conn/send-parse conn q1 nil)
        s2 (conn/send-parse conn q2 nil)

        p1 (conn/send-bind conn s1 [1] [oid/int4])
        p2 (conn/send-bind conn s2 ["hello"] [oid/text])]

    (conn/describe-statement conn s1)
    (conn/describe-portal conn p1)
    (conn/send-execute conn p1 0)

    (conn/describe-statement conn s2)
    (conn/describe-portal conn p2)
    (conn/send-execute conn p2 0)

    (conn/send-sync conn)

    (res/interact conn :execute nil)))



(deftest test-foo

  (pg/with-connection [conn CONFIG]

    (let [res (pg/batch conn)]
      (is (= 1 res))
      )

    ;; (pg/begin conn)

    ;; (pg/commit conn)
)
  )

https://github.com/pgjdbc/r2dbc-postgresql/blob/main/src/main/java/io/r2dbc/postgresql/codec/NumericDecodeUtils.java

https://stackoverflow.com/questions/38532361/converting-joda-time-instant-to-java-time-instant


(time
 (pg/with-connection [conn (assoc *CONFIG*
                                  :port 10150
                                  :binary-encode? true
                                  :binary-decode? true)]

   (pg/execute conn "select * from generate_series(1,999)" nil {:as pg.client.as/default})
   nil))


(time
 (let [conn (jdbc/get-connection (assoc *DB-SPEC* :port 10150))]
   (jdbc/execute! conn ["select * from generate_series(1,999)"])
   nil))


(doto ch
      (.setOption StandardSocketOptions/TCP_NODELAY true)
      (.setOption StandardSocketOptions/SO_KEEPALIVE true)
      (.setOption StandardSocketOptions/SO_REUSEADDR true)
      (.setOption StandardSocketOptions/SO_REUSEPORT true)

      ;; (.setOption SocketOptions/SO_TIMEOUT (int 123))
      ;; (.setOption StandardSocketOptions/)
      ;;
      ;;
      ;; SO_RCVBUF
      ;; SocketOptions
      )
