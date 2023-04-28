(ns pg.copy.jdbc
  (:import
   java.io.InputStream
   java.sql.Connection
   java.util.ArrayList
   java.util.concurrent.Executors
   java.util.concurrent.ThreadPoolExecutor
   org.postgresql.copy.CopyManager)
  (:require
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]
   [pg.copy :as copy]))


(defn by-chunks [coll n]
  (partition n n [] coll))


(defmacro with-pool [[bind threads] & body]
  `(let [~bind (Executors/newFixedThreadPool ~threads)]
     (try
       ~@body
       (finally
         (.shutdown ~bind)))))


(defmacro with-conn [[bind spec] & body]
  `(let [~bind (jdbc/get-connection ~spec)]
     (try
       ~@body
       (finally
         (.close ~bind)))))


(defn copy-in
  [connectable ^String sql data & [opt]]
  (with-conn [conn connectable]
    (let [in (copy/data->input-stream data opt)]
      (-> (new CopyManager conn)
          (.copyIn sql in)))))


(defn copy-in-parallel
  [connectable
   ^String sql
   data
   ^Integer threads
   ^Integer rows
   & [opt]]

  (with-pool [pool threads]

    (let [chunks
          (by-chunks data rows)

          futures
          (new ArrayList)]

      (doseq [chunk chunks]

        (let [fut
              (.submit pool
                       ^Callable
                       (fn []
                         (with-conn [conn connectable]
                           (let [mgr (new CopyManager conn)
                                 in (copy/data->input-stream chunk opt)]
                             (.copyIn mgr sql in)))))]

          (.add futures fut)))

      (reduce + (map deref futures)))))


(defn table-oids
  ([db table]
   (table-oids db table "public"))

  ([db table schema]
   (let [sqlvec
         (copy/sqlvec-oids table schema)

         result
         (jdbc/execute! db
                        sqlvec
                        {:builder-fn rs/as-unqualified-maps})]

     (reduce
      (fn [acc {:keys [column_name udt_name]}]
        (conj acc [(keyword column_name) udt_name]))
      []
      result))))
