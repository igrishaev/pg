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


(defn query [conn sql]
  (prot.connection/query conn sql))


#_
(comment

  (def -config {:host "127.0.0.1"
                :port 15432
                :user "foo"
                :password "foo"
                :database "ivan"})

  (with-connection [db -config]
    (query db "select 1 as foo"))

  (with-connection [db -config]
    (query db "create table aaa (id serial, title text)"))

  (with-connection [db -config]
    (query db "select * from aaa"))

  (with-connection [db -config]
    (query db "insert into aaa (title) values ('123')"))



  )
