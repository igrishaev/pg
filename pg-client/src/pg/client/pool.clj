(ns pg.client.pool
  (:require
   [pg.client.api :as api])
  (:import
   java.util.concurrent.TimeUnit
   java.util.Map
   java.util.HashMap
   java.util.concurrent.BlockingQueue
   java.util.concurrent.ArrayBlockingQueue))


(defprotocol IPool
  (idle-full? [this])
  (idle-empty? [this])
  (borrow-connection [this])
  (return-connection [this conn]))


(def QUEUE_TIMEOUT 2000)


(defn pull-conn [^BlockingQueue bq]
  (loop []
    (or (.poll bq QUEUE_TIMEOUT TimeUnit/MILLISECONDS)
        (recur))))


(defn push-conn [^BlockingQueue bq conn]
  (loop []
    (or (.offer bq conn QUEUE_TIMEOUT TimeUnit/MILLISECONDS)
        (recur))))


(defn maybe-refresh-conn [conn]
  (if (expired? conn)
    (do (api/terminate conn)
        (api/clone conn))
    conn))


(deftype Pool
    [^Map pg-config
     ^Long max-idle
     ^Long max-lifetime
     ^Object sentinel
     ^BlockingQueue conns-idle
     ^Map conns-busy]

  IPool

  (idle-full? [this]
    (>= (.size conns-idle) max-idle))

  (idle-empty? [this]
    (zero? (.size conns-idle)))

  (borrow-connection [this]

    (locking sentinel

      (let [conn
            (if (idle-empty? this)
              (api/connect pg-config)
              (-> (pull-conn conns-idle)
                  (maybe-refresh-conn)))]

        (.put conns-busy "foo" conn)

        conn)))

  (return-connection [this conn]

    (locking sentinel

      (.remove conns-busy "foo")

      (if (idle-full? this)

        (api/terminate conn)

        (let [conn (maybe-refresh-conn conn)]

          (push-conn conns-idle conn)))

      nil)))


(defmacro with-connection [[bind pool] & body]
  `(let [~bind (borrow-connection ~pool)]
     (try
       ~@body
       (finally
         (return-connection ~pool ~bind)))))



(def pool-defaults
  {:max-idle 4
   :max-lifetime 999999999999})


(defn make-pool

  ([pg-config]
   (make-pool pg-config nil))

  ([pg-config pool-config]

   (let [pool-config+
         (merge pool-defaults pool-config)

         {:keys [max-idle
                 max-lifetime]}
         pool-config+]

     (new Pool
          pg-config
          max-idle
          max-lifetime
          (new Object)
          (new ArrayBlockingQueue max-idle)
          (new HashMap)))))
