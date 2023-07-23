(ns pg.pool
  "
  A simple, non-blocking connection pool.
  It would throw an exception when being exhausted
  (out of free connections).

  Links:
  - https://github.com/psycopg/psycopg2/blob/master/lib/pool.py
  "
  (:require
   [clojure.tools.logging :as log]
   [pg.client :as pg])
  (:import
   java.io.Closeable
   java.io.Writer
   java.util.ArrayDeque
   java.util.HashMap
   java.util.List
   java.util.Map))


(defn -connect [{:keys [pg-config]}]
  (let [conn (pg/connect pg-config)]
    (log/debugf "a new connection created: %s" (pg/id conn))
    conn))


(defn -conn-expired? [{:keys [ms-lifetime]} conn]
  (let [ms-diff (- (System/currentTimeMillis)
                   (pg/created-at conn))]
    (> ms-diff ms-lifetime)))


(defn -set-closed [{:as pool :keys [^Map state]} flag]
  (.put state "closed" flag)
  pool)


(defn closed? [{:as pool :keys [^Map state]}]
  (.get state "closed"))


(defn -borrow-connection
  [{:as pool :keys [max-size
                    sentinel
                    ^ArrayDeque conns-free
                    ^Map conns-used]}]

  (locking sentinel

    (when (closed? pool)
      (throw (ex-info "the pool has been closed" {})))

    (loop []

      (if-let [conn (.poll conns-free)]

        (if (-conn-expired? pool conn)

          (do
            (log/debugf "connection %s has expired, terminating" (pg/id conn))
            (pg/terminate conn)
            (recur))

          (do
            (log/debugf "connection %s has been acquired" (pg/id conn))
            (.put conns-used (pg/id conn) conn)
            conn))

        (if (< (.size conns-used) max-size)

          (let [conn (-connect pool)]
            (.put conns-used (pg/id conn) conn)
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

    (let [id
          (pg/id conn)]

      (when-not (.remove conns-used id)
        (log/warnf "connection %s does not present in used connections" id))

      (cond

        e
        (do
          (log/debugf "terminating connection %s due to an exception" id)
          (pg/terminate conn))

        (-conn-expired? pool conn)
        (do
          (log/debugf "connection %s has expired, terminating" id)
          (pg/terminate conn))

        (pg/tx-error? conn)
        (do
          (log/debugf "connection %s is in error state, rolling back and terminating" id)
          (pg/rollback conn)
          (pg/terminate conn))

        (pg/closed? conn)
        (log/debugf "connection %s has been already closed" id)

        :else
        (do

          (when (pg/in-transaction? conn)
            (log/debugf "connection %s is in transaction, rolling back" id)
            (pg/rollback conn))

          (log/debugf "connection %s has been released" id)
          (.offer conns-free conn))))

    pool))


(defn initiate [{:as pool :keys [min-size
                                 sentinel
                                 ^ArrayDeque conns-free]}]

  (locking sentinel

    (loop [i 0]
      (when-not (= i min-size)
        (let [conn (-connect pool)]
          (.offer conns-free conn)
          (recur (inc i)))))

    (-set-closed pool false)

    pool))


(defn stats [{:as pool :keys [min-size
                              max-size
                              sentinel
                              ^ArrayDeque conns-free
                              ^Map conns-used]}]
  (locking sentinel

    {:min-size min-size
     :max-size max-size
     :free (.size conns-free)
     :used (.size conns-used)}))


(defn terminate [{:as pool :keys [sentinel
                                  ^ArrayDeque conns-free
                                  ^Map conns-used]}]

  (locking sentinel

    (when-not (closed? pool)

      (log/debug "terminating the pool...")

      (loop []
        (when-let [conn (.poll conns-free)]
          (log/debugf "terminating connection %s" (pg/id conn))
          (pg/terminate conn)
          (recur)))

      (doseq [conn (vals conns-used)]
        (log/debugf "terminating connection %s" (pg/id conn))
        (pg/terminate conn))

      (log/debug "pool termination done")

      (-set-closed pool true))

    pool))


(defrecord Pool [^Map pg-config
                 ^Long min-size
                 ^Long max-size
                 ^Long ms-lifetime
                 ^Object sentinel
                 ^ArrayDeque conns-free
                 ^Map conns-used
                 ^Map state]

  Object

  (toString [_]
    (locking sentinel
      (format "< PG pool, min: %s, max: %s, free: %s, used: %s, lifetime: %s ms >"
              min-size max-size
              (.size conns-free)
              (.size conns-used)
              ms-lifetime)))

  Closeable

  (close [this]
    (terminate this)))


(defmethod print-method Pool
  [conn ^Writer w]
  (.write w (str conn)))


(def pool-defaults
  {:min-size 2
   :max-size 8
   :ms-lifetime (* 1000 60 60 1)})


(defn -init-pool ^Pool [pg-config pool-config]

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
         (new HashMap)
         (new HashMap))))


(defn make-pool

  (^Pool [pg-config]
   (make-pool pg-config nil))

  (^Pool [pg-config pool-config]
   (initiate (-init-pool pg-config pool-config))))


(defmacro with-connection [[bind pool] & body]
  `(let [pool#
         ~pool

         ~bind
         (-borrow-connection pool#)

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
         (-return-connection pool# ~bind e#)
         (throw e#))
       (do
         (-return-connection pool# ~bind nil)
         result#))))


(defmacro with-pool
  [[bind pg-config pool-config] & body]

  `(let [~bind (make-pool ~pg-config ~pool-config)]
     (try
       ~@body
       (finally
         (terminate ~bind)))))


(defn component
  (^Pool [pg-config]
   (component pg-config nil))

  (^Pool [pg-config pool-config]

   (with-meta (-init-pool pg-config pool-config)
     {'com.stuartsierra.component/start initiate
      'com.stuartsierra.component/stop terminate})))



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
      (pg/execute conn "select 1 as one")
      (println pool)))

  )
