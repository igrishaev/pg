(ns pg.client.bench
  (:import
   com.github.igrishaev.Connection)
  (:use criterium.core)
  (:require
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as jdbc.rs]))


(def USER "wzhivga")


(comment

  ;; 8000.900308
  (let [conn (jdbc/get-connection
              {:dbtype "postgres"
               :port 15432
               :dbname USER
               :user USER
               :password USER})]

    (with-progress-reporting
      (quick-bench

          (jdbc/execute! conn
                         ["select * from generate_series(1,50000)"]
                         {:as jdbc.rs/as-unqualified-maps})

        :verbose)))


  (def ^Connection -c
    (new Connection
         "127.0.0.1"
         (int 15432)
         USER
         USER
         USER))

  (with-progress-reporting
      (quick-bench

          (.query -c "select * from generate_series(1,50000)")

          :verbose))


  #_
  (time
   (do
     (.query -c "select * from generate_series(1,5000000)")
     nil))


  )
