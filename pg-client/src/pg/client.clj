(ns pg.client
  (:require
   pg.client.auth.md5
   pg.client.auth.clear
   [pg.client.impl.connection :as impl.connection]
   [pg.client.prot.connection :as prot.connection]))


(defn get-connection [config]
  (let [conn (impl.connection/connect config)]
    (prot.connection/authenticate conn)
    (prot.connection/initiate conn)))


(defmacro with-connection [[bind config] & body]
  `(let [~bind (impl.connection/connect ~config)]
     (try
       (prot.connection/authenticate ~bind)
       (prot.connection/initiate ~bind)
       ~@body
       (finally
         (prot.connection/terminate ~bind)))))


(defn query

  ([conn sql]
   (prot.connection/query conn sql nil))

  ([conn sql {:as opt :keys [fn-column
                             fn-result
                             as-vectors?
                             as-maps?
                             as-java-maps?]}]
   (prot.connection/query conn sql opt)))


(defn begin [conn]
  (prot.connection/query conn "BEGIN" nil))


(defn rollback [conn]
  (prot.connection/query conn "ROLLBACK" nil))


(defn commit [conn]
  (prot.connection/query conn "COMMIT" nil))


(defmacro with-tx [[conn] & body]
  `(do
     (begin ~conn)
     (let [[e# result#]
           (try
             [nil (do ~@body)]
             (catch Throwable e#
               [e# nil]))]

       (if e#
         (do
           (rollback ~conn)
           (throw e#))
         (do
           (commit ~conn)
           result#)))))
