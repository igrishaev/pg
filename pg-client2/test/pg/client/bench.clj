(ns pg.client.bench
  (:import
   com.github.igrishaev.Connection)
  (:require
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as jdbc.rs]))


(def USER "wzhivga")


(comment

  ;; 8000.900308
  (time
   (let [conn (jdbc/get-connection
               {:dbtype "postgres"
                :port 15432
                :dbname USER
                :user USER
                :password USER})]

     (jdbc/execute! conn
                    ["select * from generate_series(1,5000000)"]
                    {:as jdbc.rs/as-unqualified-maps})
     nil))


  (def ^Connection -c
    (new Connection
         "127.0.0.1"
         (int 15432)
         USER
         USER
         USER))

  (time
   (do
     (.query -c "select * from generate_series(1,5000000)")
     nil))


  )
