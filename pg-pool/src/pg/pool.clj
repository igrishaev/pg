
;; TODO
;; to-string
;; print-method
;; stats methods
;; conn id gensym
;; tests

(ns pg.pool

  "
  A simple, naive connection pool.

  Links:
  - https://github.com/psycopg/psycopg2/blob/master/lib/pool.py
  "

  (:require
   [clojure.tools.logging :as log]
   [pg.client.api :as api])
  (:import

   java.util.ArrayDeque

   java.io.Closeable
   java.util.ArrayList
   java.util.HashMap
   java.util.List
   java.util.Map
   java.util.concurrent.ArrayBlockingQueue
   java.util.concurrent.BlockingQueue
   java.util.concurrent.TimeUnit))



#_
{:-conn-free 123
 :-conn-used 123
 }



(defn -initiate [{:as pool :keys [pg-config
                                  min-size
                                  sentinel
                                  ^ArrayDeque conns-free]}]

  (locking sentinel

    (loop [i 0]
      (when-not (= i min-size)
        (let [conn (api/connect pg-config)]
          (.offer conns-free conn)
          (recur (inc i)))))))


(defn -conn-expired? [{:keys [ms-lifetime]} conn]
  (let [ms-diff (- (System/currentTimeMillis)
                   (api/created-at conn))]
    (> ms-diff ms-lifetime)))


(defn -borrow-connection
  [{:as pool :keys [pg-config
                    max-size
                    sentinel
                    ^ArrayDeque conns-free
                    ^Map conns-used]}]

  (locking sentinel

    (loop []

      (if-let [conn (.poll conns-free)]

        (if (-conn-expired? pool conn)

          (do
            (log/debugf "connection %s has expired, terminating" (api/id conn))
            (api/terminate conn)
            (recur))

          (do
            (log/debugf "connection %s has been acquired" (api/id conn))
            conn))

        (if (< (.size conns-used) max-size)

          (let [conn (api/connect pg-config)]
            (log/debugf "a new connection %s has been created" (api/id conn))
            (.put conns-used (api/id conn) conn)
            conn)

          (let [msg
                (format "pool is exhausted: %s connections in use" max-size)]
            (throw (ex-info msg {:max-size max-size}))))))))


(defn -return-connection
  [{:as pool :keys [sentinel
                    ^ArrayDeque conns-free
                    ^Map conns-used]}
   conn
   e]

  (locking sentinel

    (let [id (api/id conn)]

      (.remove conns-used id)

      (cond

        e
        (do
          (log/debugf "terminating connection %s due to an exception" id)
          (api/terminate conn))

        (-conn-expired? pool conn)
        (do
          (log/debugf "connection %s has expired, terminating" id)
          (api/terminate conn))

        (api/tx-error? conn)
        (do
          (log/debugf "connection %s is in error state, rolling back and terminating" id)
          (api/rollback conn)
          (api/terminate conn))

        :else
        (do

          (when (api/in-transaction? conn)
            (log/debugf "connection %s is in transaction, rolling back" id)
            (api/rollback conn))

          (log/debugf "connection %s has been released" id)
          (.offer conns-free conn))))))


(defn terminate [{:as pool :keys [sentinel
                                  ^ArrayDeque conns-free
                                  ^Map conns-used]}]

  (locking sentinel

    ;; terminate free connections
    (loop []
      (when-let [conn (.poll conns-free)]
        (log/debugf "...")
        (api/terminate conn)
        (recur)))

    ;; terminate used connections
    (doseq [conn (vals conns-used)]
      (log/debugf "...")
      (api/terminate conn))

    ;; set closed?
    )

  pool)




(defrecord Pool [^Map pg-config
                 ^Long min-size
                 ^Long max-size
                 ^Long ms-lifetime
                 ^Object sentinel
                 ^ArrayDeque conns-free
                 ^Map conns-used])


(def pool-defaults
  {:min-size 2
   :max-size 8
   :ms-lifetime (* 1000 60 60 1)})


(defn make-pool

  ([pg-config]
   (make-pool pg-config nil))

  ([pg-config pool-config]

   (let [pool-config+
         (merge pool-defaults
                pool-config)

         {:keys [min-size
                 max-size
                 ms-lifetime]}
         pool-config+]

     (new Pool
          pg-config
          min-size
          max-size
          ms-lifetime
          (new Object)
          (new ArrayDeque)
          (new HashMap)))))





#_
(defprotocol IPool

  (new-conn [this])

  (term-conn [this conn])

  (idle-size [this])

  (idle-full? [this])

  (idle-empty? [this])

  (busy-size [this])

  (busy-full? [this])

  (busy-empty? [this])

  (poll-conn [this])

  (push-conn [this conn])

  (conn-expired? [this conn])

  (borrow-connection [this])

  (return-connection [this conn e])

  (terminate [this]))


#_
(deftype Pool
    [^Map -pg-config
     ^Long -size
     ^Long -lifetime
     ^Long -timeout
     ^Object -sentinel
     ^BlockingQueue -conns-idle
     ^Map -conns-busy]

    IPool

    (new-conn [this]
      (let [conn (api/connect -pg-config)]
        (log/debugf "a new connection has been created: %s/%s"
                    (api/id conn) (api/created-at conn))
        conn))

    (term-conn [this conn]
      (log/debugf "connection %s/%s has been terminated"
                  (api/id conn) (api/created-at conn))
      (api/terminate conn)
      nil)

    (conn-expired? [this conn]
      (> (- (System/currentTimeMillis)
            (api/created-at conn))
         -lifetime))

    (idle-size [this]
      (.size -conns-idle))

    (idle-full? [this]
      (>= (idle-size this) -size))

    (idle-empty? [this]
      (zero? (idle-size this)))

    (busy-size [this]
      (.size -conns-busy))

    (busy-full? [this]
      (>= (busy-size this) -size))

    (busy-empty? [this]
      (zero? (busy-size this)))

    (poll-conn [this]
      (loop []
        (or (.poll -conns-idle
                   -timeout
                   TimeUnit/MILLISECONDS)
            (recur))))

    (push-conn [this conn]
      (loop []
        (or (.offer -conns-idle
                    conn
                    -timeout
                    TimeUnit/MILLISECONDS)
            (recur))))

    (borrow-connection [this]

      (locking -sentinel

        (let [new?
              (and (idle-empty? this)
                   (not (busy-full? this)))

              conn
              (if new?
                (new-conn this)
                (let [conn (poll-conn this)]
                  (if (conn-expired? this conn)
                    (do
                      (term-conn this conn)
                      (new-conn this)))
                  conn))]

          (.put -conns-busy (api/id conn) conn)

          conn)))

    (return-connection [this conn e]

      (locking -sentinel

        (.remove -conns-busy (api/id conn))

        (let [term?
              (or e
                  (conn-expired? this conn)
                  (idle-full? this))]

          (if term?
            (term-conn this conn)
            (push-conn this conn)))

        nil))

    (terminate [this]

      (log/debugf "terminating the pool...")

      (let [array (new ArrayList)]
        (.drainTo -conns-idle array)

        (doseq [conn array]
          (term-conn this conn)))

      (doseq [conn (vals -conns-busy)]
        (term-conn this conn))

      (log/debugf "the pool gas been terminated")

      nil)

    Closeable

    (close [this]
      (terminate this)))


#_
(defmacro with-connection [[bind pool] & body]
  `(let [pool#
         ~pool

         ~bind
         (borrow-connection pool#)

         ^List pair#
         (try
           [(do ~@body) nil]
           (catch Throwable e#
             [nil e#]))

         result#
         (.get pair# 0)

         e#
         (.get pair# 1)]

     (if e#
       (do
         (return-connection pool# ~bind e#)
         (throw e#))
       (do
         (return-connection pool# ~bind nil)
         result#))))


#_
(def pool-defaults
  {:size 4
   :lifetime (* 1000 60 60 1)
   :timeout (* 1000 2)})


#_
(defn make-pool

  (^Pool [pg-config]
   (make-pool pg-config nil))

  (^Pool [pg-config pool-config]

   (let [pool-config+
         (merge pool-defaults pool-config)

         {:keys [size
                 lifetime
                 timeout]}
         pool-config+

         pool
         (new Pool
              pg-config
              size
              lifetime
              timeout
              (new Object)
              (new ArrayBlockingQueue size)
              (new HashMap))]

     (log/debugf "a new PG connection pool has been created")

     pool)))


#_
(defmacro with-pool [[bind pg-config pool-config]
                     & body]

  `(let [~bind (make-pool ~pg-config ~pool-config)]
     (try
       ~@body
       (finally
         (terminate ~bind)))))


#_
(comment

  (def PG_CONFIG
    {:host "127.0.0.1"
     :port 15432
     :user "ivan"
     :password "ivan"
     :database "ivan"})

  (with-pool [pool PG_CONFIG]
    (with-connection [conn pool]
      (api/query conn "select 1 as one")))

  )
