

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

https://github.com/pgjdbc/pgjdbc/issues/1311#issuecomment-1143805011

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


<-  {:msg :ErrorResponse, :errors {:severity ERROR, :verbosity ERROR, :code 08P01, :message unexpected message type 0x43 during COPY from stdin, :stacktrace COPY foo, line 1, :file copyfromparse.c, :line 292, :function CopyGetData}}
<-  {:msg :ErrorResponse, :errors {:severity FATAL, :verbosity FATAL, :code 08P01, :message terminating connection because protocol synchronization was lost, :file postgres.c, :line 4369, :function PostgresMain}}


16:20:35 DEBUG pg.pool - a new connection created: pg10177
16:20:35 DEBUG pg.pool - connection pg10177 has been acquired
16:20:35 DEBUG pg.pool - a new connection created: pg10178
16:20:35 DEBUG pg.pool - connection pg10177 has been acquired
...
16:20:35 DEBUG pg.pool - connection pg10177 has been released
16:20:35 DEBUG pg.pool - terminating the pool...
16:20:35 DEBUG pg.pool - terminating connection pg10178
16:20:35 DEBUG pg.pool - terminating connection pg10177
16:20:35 DEBUG pg.pool - pool termination done

{:array "{NULL,\"null\",\"NULL\",NULL}"}

typed arrays
java.util.List?
java.lang.Iterable?

Class ofArray = o.getClass().getComponentType();


(def IntArray
  (type (int-array 0)))


(def -spec
  {:dbtype "postgres"
   :port 10140
   :dbname "test"
   :user "test"
   :password "test"})

(next.jdbc/execute! -spec ["select '{1,2,3}'::int[] as arr"])
#_[{:arr #object[org.postgresql.jdbc.PgArray 0x487ba693 "{1,2,3}"]}]

(next.jdbc/execute! -spec ["select '{{1,2},{3,4}}'::int[] as arr"])
#_[{:arr #object[org.postgresql.jdbc.PgArray 0x768c120c "{{1,2},{3,4}}"]}]

(next.jdbc/execute! -spec ["select '{now(),now()}'::timestamp[] as arr"])
#_[{:arr
  #object[org.postgresql.jdbc.PgArray 0x3d72a56b "{\"2023-09-13 15:45:31.55814\",\"2023-09-13 15:45:31.55814\"}"]}]


ssl
https://gist.github.com/achesco/b893fb55b90651cf5f4cc803b78e19fd
https://docs.exalate.com/docs/how-to-secure-a-connection-between-exalate-and-a-postgresql-database-in-docker
https://github.com/pgjdbc/pgjdbc/blob/5709a20fbef453749d2394e11502527e4a3ab5bb/pgjdbc/src/main/java/org/postgresql/ssl/MakeSSL.java#L26
https://stackoverflow.com/questions/55072221/deploying-postgresql-docker-with-ssl-certificate-and-key-with-volumes
https://www.crunchydata.com/blog/ssl-certificate-authentication-postgresql-docker-containers

(require '[less.awful.ssl :as ssl])

(.setUseClientMode -s true)
(.startHandshake -s)

(def -s (-> (ssl/ssl-context "/Users/ivan/work/pg/certs/client.key" "/Users/ivan/work/pg/certs/client.crt" "/Users/ivan/work/pg/certs/root.crt")
            (ssl/socket "localhost" 10130)))


[0, 0, 0, 8, 4, -46, 22, 47]

(def -in (.getInputStream -s))

(def -out (.getOutputStream -s))

(.write -out (byte-array [0, 0, 0, 8, 4, -46, 22, 47]))

https://stackoverflow.com/questions/8425999/upgrade-java-socket-to-encrypted-after-issue-starttls

SSLSocket sslsocket = (SSLSocket) sslsocketfactory.createSocket(
                                                                socket,
                                                                socket.getInetAddress().getHostAddress(),
                                                                socket.getPort(),
                                                                true) ;

ssl = on
ssl_cert_file = '/Users/ivan/work/pg/certs/server.crt'
ssl_key_file = '/Users/ivan/work/pg/certs/server.key'
ssl_ca_file = '/Users/ivan/work/pg/certs/root.crt'

hostssl all	all	all           cert #clientcert=verify-full

psql "host=localhost dbname=ivan user=chesco port=15432 sslmode=verify-full sslcert=/Users/ivan/work/pg/certs/client.crt sslkey=/Users/ivan/work/pg/certs/client.key sslrootcert=/Users/ivan/work/pg/certs/root.crt"


(encode-StartupMessage {:protocol-version 196608
                          :user "chesco"
                          :database "ivan"}
                         nil)

[0, 0, 0, 35, 0, 3, 0, 0, 117, 115, 101, 114, 0, 99, 104, 101, 115, 99, 111, 0,
 100, 97, 116, 97, 98, 97, 115, 101, 0, 105, 118, 97, 110, 0, 0]


(comment

  (def -s (new Socket "localhost" 15432 true))

  (def -out (.getOutputStream -s))
  (def -in (.getInputStream -s))

  (.write -out (byte-array [0, 0, 0, 8, 4, -46, 22, 47]))
  (.read -in)

  (require '[less.awful.ssl :as ssl])

  (def -sf (SSLSocketFactory/getDefault))

  (def -ctx
    (ssl/ssl-context "/Users/ivan/work/pg/certs/client.key"
                     "/Users/ivan/work/pg/certs/client.crt"
                     "/Users/ivan/work/pg/certs/root.crt"))

  (def -sf
    (.getSocketFactory -ctx))

  (def -ss (.createSocket -sf -s "localhost" 15432 true))

  (.setUseClientMode -ss true)
  (.startHandshake -ss)

  #_
  (def -ch (.getChannel -ss))

  (def -sout (.getOutputStream -ss))
  (def -sin (.getInputStream -ss))

  (.write -sout (byte-array [0, 0, 0, 35, 0, 3, 0, 0, 117, 115, 101, 114, 0, 99, 104, 101, 115, 99, 111, 0, 100, 97, 116, 97, 98, 97, 115, 101, 0, 105, 118, 97, 110, 0, 0]))

  (def -buf (byte-array 32))

  (.read -sin -buf)

  )


(def USER "chesco")

(def PASS "chesco")

(def DATABASE "ivan")


(def ^:dynamic *CONFIG*
  {:host HOST
   :port nil
   :user USER
   :password PASS
   :database DATABASE
   :ssl {:key-file "/Users/ivan/work/pg/certs/client.key"
         :cert-file "/Users/ivan/work/pg/certs/client.crt"
         :ca-cert-file "/Users/ivan/work/pg/certs/root.crt"}})


(:require
   [less.awful.ssl :as ssl])

(def enabled-protocols
  (into-array String ["TLSv1.2" "TLSv1.1" "TLSv1"]))

#_
{:keys [key-file
        cert-file
        ca-cert-file]}
#_
ssl-opt

;; ^SSLContext ssl-context

#_
(if ca-cert-file
  (ssl/ssl-context key-file cert-file ca-cert-file)
  (ssl/ssl-context key-file cert-file))

;; socket-factory
;; (.getSocketFactory ssl-context)

[less-awful-ssl]


(deftest test-custom-ssl-context

  (let [ssl-context
        (ssl/context {:key-file "../certs/client.key"
                      :cert-file "../certs/client.crt"
                      :ca-cert-file "../certs/root.crt"})]

    (pg/with-connection [conn (assoc *CONFIG*
                                     :port 15432
                                     :ssl? true
                                     :ssl-context ssl-context)]

      (is (pg/ssl? conn))

      (is (= [{:foo 1}]
             (pg/query conn "select 1 as foo"))))))
