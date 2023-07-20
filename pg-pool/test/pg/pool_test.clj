(ns pg.pool-test
  (:require
   [com.stuartsierra.component :as component]
   [clojure.test :refer [is deftest]]
   [pg.pool :as pool]
   [pg.client.api :as api]))


(def PG_CONFIG
  {:host "127.0.0.1"
   :port 15432
   :user "ivan"
   :password "ivan"
   :database "ivan"})


(deftest test-pool-it-works
  (pool/with-pool [pool PG_CONFIG]
    (pool/with-connection [conn pool]
      (let [res (api/execute conn "select 1 as one")]
        (is (= [{:one 1}] res))))))


(deftest test-pool-basic-features
  (pool/with-pool [pool PG_CONFIG {:max-size 2}]

    (let [t1-conn-id
          (promise)

          t1-start
          (promise)

          t1-stop
          (promise)

          t2-start
          (promise)

          t2-stop
          (promise)

          f1
          (future
            (pool/with-connection [conn pool]
              (deliver t1-start true)
              (deliver t1-conn-id (api/id conn))
              (Thread/sleep 1000)
              (api/execute conn "select 1 as one"))
            (deliver t1-stop true))

          f2
          (future
            (pool/with-connection [conn pool]
              (deliver t2-start true)
              (Thread/sleep 2000)
              (api/execute conn "select 2 as two"))
            (deliver t2-stop true))]

      @t1-start
      @t2-start

      (is (= {:min-size 2 :max-size 2 :free 0 :used 2}
             (pool/stats pool)))

      (try
        (pool/with-connection [conn pool]
          (api/execute conn "select 42"))
        (is false)
        (catch Exception e
          (is (= "pool is exhausted: 2 connections in use"
                 (ex-message e)))))

      @t1-stop

      (is (= {:min-size 2 :max-size 2 :free 1 :used 1}
             (pool/stats pool)))

      (let [res
            (pool/with-connection [conn pool]
              (is (= @t1-conn-id (api/id conn)))
              (api/execute conn "select 3 as three"))]
        (is (= [{:three 3}] res)))

      @t2-stop

      (is (= {:min-size 2 :max-size 2 :free 2 :used 0}
             (pool/stats pool))))))


(deftest test-pool-lifetime
  (pool/with-pool [pool PG_CONFIG {:min-size 2
                                   :max-size 2
                                   :ms-lifetime 300}]

    (is (= {:min-size 2 :max-size 2 :free 2 :used 0}
           (pool/stats pool)))

    (Thread/sleep 500)

    (is (= {:min-size 2 :max-size 2 :free 2 :used 0}
           (pool/stats pool)))

    (pool/with-connection [conn pool]

      (is (= {:min-size 2 :max-size 2 :free 0 :used 1}
           (pool/stats pool)))

      (api/execute conn "select 1 as one"))

    (is (= {:min-size 2 :max-size 2 :free 1 :used 0}
           (pool/stats pool)))))


(deftest test-pool-exception-terminated
  (pool/with-pool [pool PG_CONFIG {:min-size 1
                                   :max-size 1}]

    (let [id1
          (promise)

          id2
          (promise)

          id3
          (promise)]

      (pool/with-connection [conn pool]
        (deliver id1 (api/id conn)))

      (pool/with-connection [conn pool]
        (deliver id2 (api/id conn)))

      (is (= @id1 @id2))

      (try
        (pool/with-connection [conn pool]
          (is (= @id2 (api/id conn)))
          (/ 0 0))
        (catch Exception e
          (is e)))

      (pool/with-connection [conn pool]
        (deliver id3 (api/id conn)))

      (is (not= @id1 @id3)))))


(deftest test-pool-in-transaction-state
  (pool/with-pool [pool PG_CONFIG {:min-size 1
                                   :max-size 1}]

    (let [id1
          (promise)

          id2
          (promise)

          id3
          (promise)

          id4
          (promise)]

      (pool/with-connection [conn pool]
        (deliver id1 (api/id conn)))

      (pool/with-connection [conn pool]
        (deliver id2 (api/id conn)))

      (pool/with-connection [conn pool]
        (api/begin conn)
        (deliver id3 (api/id conn))
        (is (api/in-transaction? conn)))

      (pool/with-connection [conn pool]
        (is (api/idle? conn))
        (deliver id4 (api/id conn)))

      (is (= @id1 @id2 @id3 @id4)))))


(deftest test-pool-in-error-state
  (pool/with-pool [pool PG_CONFIG {:min-size 1
                                   :max-size 1}]

    (let [id1
          (promise)

          id2
          (promise)

          id3
          (promise)]

      (pool/with-connection [conn pool]
        (deliver id1 (api/id conn)))

      (pool/with-connection [conn pool]
        (api/begin conn)
        (try
          (api/execute conn "selekt 42")
          (is false)
          (catch Exception e
            (is (api/tx-error? conn))))
        (deliver id2 (api/id conn)))

      (pool/with-connection [conn pool]
        (is (api/idle? conn))
        (deliver id3 (api/id conn)))

      (is (= @id1 @id2))
      (is (not= @id2 @id3)))))


(deftest test-pool-component

  (let [c
        (pool/component PG_CONFIG)

        _
        (is (not (pool/started? c)))

        stats1
        (pool/stats c)

        c-started
        (component/start c)

        _
        (is (pool/started? c-started))

        stats2
        (pool/stats c-started)]

    (is (= {:min-size 2 :max-size 8 :free 0 :used 0}
           stats1))

    (is (= {:min-size 2 :max-size 8 :free 2 :used 0}
           stats2))

    (pool/with-connection [conn c-started]
      (let [res (api/execute conn "select 1 as one")]
        (is (= [{:one 1}] res))))

    (let [c-stopped
          (component/stop c-started)

          stats3
          (pool/stats c-stopped)]

      (is (not (pool/started? c-stopped)))

      (is (= {:min-size 2 :max-size 8 :free 0 :used 0}
             stats3)))))


(deftest test-pool-component-redundant-start

  (let [c-started
        (-> (pool/component PG_CONFIG)
            (component/start)
            (component/start)
            (component/start))]

    (pool/with-connection [conn c-started]
      (let [res (api/execute conn "select 1 as one")]
        (is (= [{:one 1}] res))))

    (let [c-stopped
          (-> c-started
              (component/stop)
              (component/stop)
              (component/stop))]

      (is (not (pool/started? c-stopped))))))


(deftest test-pool-with-open
  (with-open [pool (pool/make-pool PG_CONFIG)]
    (pool/with-connection [conn pool]
      (let [res (api/execute conn "select 1 as one")]
        (is (= [{:one 1}] res))))))


(deftest test-pool-conn-terminated

  (let [id1
        (promise)

        id2
        (promise)

        id3
        (promise)

        id4
        (promise)

        id5
        (promise)

        pool-config
        {:min-size 0 :max-size 1}]

    (with-open [pool (pool/make-pool PG_CONFIG pool-config)]

      (pool/with-connection [conn pool]
        (deliver id1 (api/id conn)))

      (pool/with-connection [conn pool]
        (api/terminate conn)
        (deliver id2 (api/id conn)))

      (pool/with-connection [conn pool]
        (deliver id3 (api/id conn))
        (let [res (api/execute conn "select 1 as one")]
          (is (= [{:one 1}] res))))

      (pool/with-connection [conn pool]
        (deliver id4 (api/id conn)))

      (is (= @id1 @id2))
      (is (not= @id2 @id3))
      (is (= @id3 @id4)))))


(deftest test-pool-termination

  (pool/with-pool [pool PG_CONFIG]
    (pool/terminate pool)

    (try
      (pool/with-connection [conn pool]
        42)
      (is false)
      (catch Exception e
        (is (= "the pool has not been started" (ex-message e)))))))


;; pool refactor closed? state
;; test reuse closed conn
;; test reuse closed pool
