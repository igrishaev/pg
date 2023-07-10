
;; to-string
;; print-method
;; stats methods

(ns pg.client.pool
  (:require
   [pg.client.api :as api])
  (:import
   java.io.Closeable
   java.util.ArrayList
   java.util.HashMap
   java.util.List
   java.util.Map
   java.util.concurrent.ArrayBlockingQueue
   java.util.concurrent.BlockingQueue
   java.util.concurrent.TimeUnit))


(defprotocol IPool

  (new-conn [this])

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
    (api/connect -pg-config))

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
                  (do (api/terminate conn)
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
          (api/terminate conn)
          (push-conn this conn)))

      nil))

  (terminate [this]

    (let [array (new ArrayList)]
      (.drainTo -conns-idle array)

      (doseq [conn array]
        (api/terminate conn)))

    (doseq [conn (vals -conns-busy)]
      (api/terminate conn))

    nil)

  Closeable

  (close [this]
    (terminate this)))


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


(def pool-defaults
  {:size 4
   :lifetime (* 1000 60 60 1)
   :timeout (* 1000 2)})


(defn make-pool

  ([pg-config]
   (make-pool pg-config nil))

  ([pg-config pool-config]

   (let [pool-config+
         (merge pool-defaults pool-config)

         {:keys [size
                 lifetime
                 timeout]}
         pool-config+]

     (new Pool
          pg-config
          size
          lifetime
          timeout
          (new Object)
          (new ArrayBlockingQueue size)
          (new HashMap)))))


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
