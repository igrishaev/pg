(ns pg.copy.jdbc
  (:import
   java.sql.Connection
   java.sql.PreparedStatement
   java.util.ArrayList
   java.util.Map
   java.util.concurrent.Executors
   java.util.concurrent.ThreadPoolExecutor
   javax.sql.DataSource
   org.postgresql.copy.CopyManager)
  (:require
   [pg.copy :as copy]))


(defn by-chunks [coll n]
  (partition n n [] coll))


(defmacro with-pool [[bind threads] & body]
  `(let [~bind (Executors/newFixedThreadPool ~threads)]
     (try
       ~@body
       (finally
         (.shutdown ~bind)))))


(defn get-jdbc-url [{:keys [port
                            host
                            dbname]
                     :or {host "127.0.0.1"
                          port 5432}}]
  (format "jdbc:postgresql://%s:%d/%s" host port dbname))


(defn copy-in
  [^Connection conn ^String sql data & [opt]]
  (let [in (copy/data->input-stream data opt)]
    (-> (new CopyManager conn)
        (.copyIn sql in))))


(defn copy-in-parallel
  [^DataSource datasource
   ^String sql
   data
   ^Integer threads
   ^Integer rows
   & [opt]]

  (with-pool [pool threads]

    (let [chunks
          (by-chunks data rows)

          meta'
          (meta data)

          futures
          (new ArrayList)]

      (doseq [chunk chunks]

        (let [chunk'
              (with-meta chunk meta')

              fut
              (.submit pool
                       ^Callable
                       (fn []
                         (with-open [conn (.getConnection datasource)]
                           (let [mgr (new CopyManager conn)
                                 in (copy/data->input-stream chunk' opt)]
                             (.copyIn mgr sql in)))))]

          (.add futures fut)))

      (reduce + (map deref futures)))))


(defn enumerate [coll]
  (map-indexed vector coll))


(defn table-oids
  ([^Connection conn table]
   (table-oids conn table "public"))

  ([^Connection conn table schema]
   (let [sqlvec
         (copy/sqlvec-oids table schema)

         [query & args]
         sqlvec

         ^PreparedStatement stmt
         (.prepareStatement conn query)

         _
         (doseq [[i arg] (enumerate args)]
           (.setString stmt (inc i) arg))

         rs
         (.executeQuery stmt)]

     (reduce
      (fn [acc {:keys [column_name udt_name]}]
        (conj acc [(keyword column_name) udt_name]))
      []
      (resultset-seq rs)))))
