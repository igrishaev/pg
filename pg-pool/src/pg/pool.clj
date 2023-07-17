
;; TODO
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
   java.io.Writer
   java.io.Closeable
   java.util.HashMap
   java.util.List
   java.util.Map))


(defn -connect [{:keys [pg-config]}]
  (let [conn (api/connect pg-config)]
    (log/debugf "a new connection created: %s" (api/id conn))
    conn))


(defn -initiate [{:as pool :keys [min-size
                                  sentinel
                                  ^ArrayDeque conns-free]}]

  (locking sentinel

    (loop [i 0]
      (when-not (= i min-size)
        (let [conn (-connect pool)]
          (.offer conns-free conn)
          (recur (inc i)))))

    pool))


(defn -conn-expired? [{:keys [ms-lifetime]} conn]
  (let [ms-diff (- (System/currentTimeMillis)
                   (api/created-at conn))]
    (> ms-diff ms-lifetime)))


(defn -borrow-connection
  [{:as pool :keys [max-size
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
            (.put conns-used (api/id conn) conn)
            conn))

        (if (< (.size conns-used) max-size)

          (let [conn (-connect pool)]
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

    (let [id
          (api/id conn)]

      (when-not (.remove conns-used id)
        (log/warnf "connection %s does not present in used connections" id))

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


(defn set-closed [{:as pool :keys [^Map state]}]
  (.put state "closed" true)
  pool)


(defn closed? [{:as pool :keys [^Map state]}]
  (.get state "closed"))


(defn terminate [{:as pool :keys [sentinel
                                  ^ArrayDeque conns-free
                                  ^Map conns-used]}]

  (locking sentinel

    (when-not (closed? pool)

      (log/debug "terminating the pool...")

      (loop []
        (when-let [conn (.poll conns-free)]
          (log/debugf "terminating connection %s" (api/id conn))
          (api/terminate conn)
          (recur)))

      (doseq [conn (vals conns-used)]
        (log/debugf "terminating connection %s" (api/id conn))
        (api/terminate conn))

      (set-closed pool)

      (log/debug "pool termination done")

      pool)))


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
              ms-lifetime))))


(defmethod print-method Pool
  [conn ^Writer w]
  (.write w (str conn)))


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
         pool-config+

         pool
         (new Pool
              pg-config
              min-size
              max-size
              ms-lifetime
              (new Object)
              (new ArrayDeque)
              (new HashMap)
              (new HashMap))]

     (-initiate pool))))


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
      (api/execute conn "select 1 as one")
      (println pool)))

  )
