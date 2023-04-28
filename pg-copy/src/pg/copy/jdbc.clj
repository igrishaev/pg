(ns pg.copy.jdbc
  (:import
   java.sql.Connection
   java.io.InputStream
   org.postgresql.copy.CopyManager)
  (:require
   [pg.copy :as copy]))


(defn copy-in

  ([^Connection conn ^String sql data]
   (copy-in conn sql data nil))

  ([^Connection conn ^String sql data opt]
   (let [in (copy/data->input-stream data opt)]
     (-> (new CopyManager conn)
         (.copyIn sql in)))))
