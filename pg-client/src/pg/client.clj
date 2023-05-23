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

  ([conn sql opt]
   (prot.connection/query conn sql opt)))
