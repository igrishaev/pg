


(time
 (with-connection [db -config]
   (doseq [_ (range 10000)]
     (connection/query db "select * from aaa"))))


(time
 (let [conn (next.jdbc/get-connection {:dbtype "postgres" :dbname "ivan" :user "ivan" :password "ivan" :port 15432})]
   (doseq [_ (range 10000)]
     (next.jdbc/execute! conn ["select * from aaa"]))))
