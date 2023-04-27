(ns pg.copy.jdbc
  (:import
   java.sql.Connection
   java.io.InputStream
   org.postgresql.copy.CopyManager)
  (:require
   [pg.copy :as copy]))


(defn copy-in

  ([^Connection conn ^String sql]
   (-> (new CopyManager conn)
       (.copyIn sql)))

  ([^Connection conn ^String sql ^InputStream in]
   (-> (new CopyManager conn)
       (.copyIn sql in))))
