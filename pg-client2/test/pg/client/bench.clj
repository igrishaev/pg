(ns pg.client.bench
  (:import
   com.github.igrishaev.Connection
   com.github.igrishaev.Config$Builder)
  (:use criterium.core)
  (:require
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as jdbc.rs]))


;; (def USER "wzhivga")
(def USER "ivan")


(def cfg
  (-> (new Config$Builder USER USER)
      (.host "127.0.0.1")
      (.port 15432)
      (.password USER)
      (.binaryEncode false)
      (.binaryDecode false)
      (.build)))


(comment

  (time
   (do
     (let [conn (jdbc/get-connection
                 {:dbtype "postgres"
                  :port 15432
                  :dbname USER
                  :user USER
                  :password USER})]

       (jdbc/execute! conn
                      ["select * from generate_series(1,5000000)"]
                      #_
                      {:as jdbc.rs/as-unqualified-maps}))
     nil))

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


  #_
  (def ^Connection -c
    (new Connection
         "127.0.0.1"
         (int 15432)
         USER
         USER
         USER))

  (def ^Connection -c
    (new Connection cfg))


  (time
   (do
     (.execute -c "select * from generate_series(1,5000000)")
     nil))

  (time
   (do
     (.query -c "select * from generate_series(1,5000000)")
     nil))

  (with-progress-reporting
    (quick-bench
        (.execute -c "select * from generate_series(1,50000)")
        :verbose))


  #_
  (time
   (do
     (.query -c "select * from generate_series(1,5000000)")
     nil))


  )
