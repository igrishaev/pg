(ns pg.pool
  (:require
   [pg.client :as pg])
  (:import
   com.github.igrishaev.Connection
   com.github.igrishaev.pool.Pool
   com.github.igrishaev.pool.PoolConfig
   com.github.igrishaev.pool.PoolConfig$Builder
   java.io.Writer
   java.util.Map))


(defn ->pool-config ^PoolConfig [opt]

  (let [{:keys [min-size
                max-size
                max-lifetime]}
        opt]

    (cond-> (PoolConfig/builder)

      min-size
      (.minSize min-size)

      max-size
      (.maxSize max-size)

      max-lifetime
      (.maxLifetime max-lifetime)

      :finally
      (.build))))


(defn pool

  (^Pool [^Map conn-config]
   (new Pool (pg/->conn-config conn-config)))

  (^Pool [^Map conn-config ^Map pool-config]
   (new Pool
        (pg/->conn-config conn-config)
        (->pool-config pool-config))))


(defn close [^Pool pool]
  (.close pool))


(defn used-count ^Integer [^Pool pool]
  (.usedCount pool))


(defn free-count ^Integer [^Pool pool]
  (.freeCount pool))


(defmacro with-pool
  [[bind conn-config pool-config] & body]
  `(with-open [~bind (pool ~conn-config ~pool-config)]
     ~@body))


(defmacro with-connection [[bind pool] & body]
  `(let [pool#
         ~pool

         ~(with-meta bind {:tag `Connection})
         (.borrowConnection pool#)]
     (try
       ~@body
       (finally
         (.returnConnection pool# ~bind)))))


(defn stats [^Pool pool]
  {:free (free-count pool)
   :used (used-count pool)})


(defn closed? ^Boolean [^Pool pool]
  (.isClosed pool))


(defmethod print-method Pool
  [^Pool pool ^Writer writer]
  (.write writer (.toString pool)))
