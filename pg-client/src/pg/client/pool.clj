(ns pg.client.pool
  (:require
   [pg.client.api :as api])
  (:import
   java.util.concurrent.TimeUnit
   java.util.Map
   java.util.concurrent.ArrayBlockingQueue))


(defprotocol IPool
  (borrow-connection [this])
  (return-connection [this conn]))


(deftype Pool
    [^Long max-size
     ^Long min-size
     ^Long max-lifetime
     ^Object sentinel
     ^ArrayBlockingQueue conns-free
     ^ArrayBlockingQueue conns-busy]

  IPool

  (borrow-connection [this]

    ;; of expired?
    ;; close conn
    ;; create conn

    (locking sentinel

      (let [conn
            (.poll conns-free 20000 TimeUnit/MILLISECONDS)

            result
            (.offer conns-busy conn 20000 TimeUnit/MILLISECONDS)]

        conn)))

  (return-connection [this conn]

    (locking sentinel

      ;; if expired?
      ;; close conn
      ;; create conn

      (let [flag
            (.remove conns-busy conn)

            flag
            (.offer conns-busy conn 20000 TimeUnit/MILLISECONDS)]

        nil))))



(defn make-pool [config]

  (let [{:keys [max-size
                min-size
                max-lifetime]}
        config]

    (new Pool
         max-size
         min-size
         max-lifetime
         (new Object)
         (new ArrayBlockingQueue max-size)
         (new ArrayBlockingQueue max-size))))
