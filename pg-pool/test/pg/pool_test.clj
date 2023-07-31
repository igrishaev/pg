(ns pg.pool-test
  (:require
   [com.stuartsierra.component :as component]
   [clojure.test :refer [is deftest use-fixtures]]
   [pg.integration :as pgi :refer [*CONFIG*]]
   [pg.pool :as pool]
   [pg.client :as pg]))


(use-fixtures :each pgi/fix-multi-version)


(deftest test-pool-it-works
  (pool/with-pool [pool *CONFIG*]
    (pool/with-connection [conn pool]
      (let [res (pg/execute conn "select 1 as one")]
        (is (= [{:one 1}] res))))))


(deftest test-pool-basic-features
  (pool/with-pool [pool *CONFIG* {:max-size 2}]

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
              (deliver t1-conn-id (pg/id conn))
              (Thread/sleep 1000)
              (pg/execute conn "select 1 as one"))
            (deliver t1-stop true))

          f2
          (future
            (pool/with-connection [conn pool]
              (deliver t2-start true)
              (Thread/sleep 2000)
              (pg/execute conn "select 2 as two"))
            (deliver t2-stop true))]

      @t1-start
      @t2-start

      (is (= {:min-size 2 :max-size 2 :free 0 :used 2}
             (pool/stats pool)))

      (try
        (pool/with-connection [conn pool]
          (pg/execute conn "select 42"))
        (is false)
        (catch Exception e
          (is (= "pool is exhausted: 2 connections in use"
                 (ex-message e)))))

      @t1-stop

      (is (= {:min-size 2 :max-size 2 :free 1 :used 1}
             (pool/stats pool)))

      (let [res
            (pool/with-connection [conn pool]
              (is (= @t1-conn-id (pg/id conn)))
              (pg/execute conn "select 3 as three"))]
        (is (= [{:three 3}] res)))

      @t2-stop

      (is (= {:min-size 2 :max-size 2 :free 2 :used 0}
             (pool/stats pool))))))


(deftest test-pool-lifetime
  (pool/with-pool [pool *CONFIG* {:min-size 2
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

      (pg/execute conn "select 1 as one"))

    (is (= {:min-size 2 :max-size 2 :free 1 :used 0}
           (pool/stats pool)))))


(deftest test-pool-exception-terminated
  (pool/with-pool [pool *CONFIG* {:min-size 1
                                   :max-size 1}]

    (let [id1
          (promise)

          id2
          (promise)

          id3
          (promise)]

      (pool/with-connection [conn pool]
        (deliver id1 (pg/id conn)))

      (pool/with-connection [conn pool]
        (deliver id2 (pg/id conn)))

      (is (= @id1 @id2))

      (try
        (pool/with-connection [conn pool]
          (is (= @id2 (pg/id conn)))
          (/ 0 0))
        (catch Exception e
          (is e)))

      (pool/with-connection [conn pool]
        (deliver id3 (pg/id conn)))

      (is (not= @id1 @id3)))))


(deftest test-pool-in-transaction-state
  (pool/with-pool [pool *CONFIG* {:min-size 1
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
        (deliver id1 (pg/id conn)))

      (pool/with-connection [conn pool]
        (deliver id2 (pg/id conn)))

      (pool/with-connection [conn pool]
        (pg/begin conn)
        (deliver id3 (pg/id conn))
        (is (pg/in-transaction? conn)))

      (pool/with-connection [conn pool]
        (is (pg/idle? conn))
        (deliver id4 (pg/id conn)))

      (is (= @id1 @id2 @id3 @id4)))))


(deftest test-pool-in-error-state
  (pool/with-pool [pool *CONFIG* {:min-size 1
                                   :max-size 1}]

    (let [id1
          (promise)

          id2
          (promise)

          id3
          (promise)]

      (pool/with-connection [conn pool]
        (deliver id1 (pg/id conn)))

      (pool/with-connection [conn pool]
        (pg/begin conn)
        (try
          (pg/execute conn "selekt 42")
          (is false)
          (catch Exception e
            (is (pg/tx-error? conn))))
        (deliver id2 (pg/id conn)))

      (pool/with-connection [conn pool]
        (is (pg/idle? conn))
        (deliver id3 (pg/id conn)))

      (is (= @id1 @id2))
      (is (not= @id2 @id3)))))


(deftest test-pool-component

  (let [c
        (pool/component *CONFIG*)

        _
        (is (not (pool/closed? c)))

        stats1
        (pool/stats c)

        c-started
        (component/start c)

        _
        (is (not (pool/closed? c-started)))

        stats2
        (pool/stats c-started)]

    (is (= {:min-size 2 :max-size 8 :free 0 :used 0}
           stats1))

    (is (= {:min-size 2 :max-size 8 :free 2 :used 0}
           stats2))

    (pool/with-connection [conn c-started]
      (let [res (pg/execute conn "select 1 as one")]
        (is (= [{:one 1}] res))))

    (let [c-stopped
          (component/stop c-started)

          stats3
          (pool/stats c-stopped)]

      (is (pool/closed? c-stopped))

      (is (= {:min-size 2 :max-size 8 :free 0 :used 0}
             stats3)))))


(deftest test-pool-component-redundant-start

  (let [c-started
        (-> (pool/component *CONFIG*)
            (component/start)
            (component/start)
            (component/start))]

    (pool/with-connection [conn c-started]
      (let [res (pg/execute conn "select 1 as one")]
        (is (= [{:one 1}] res))))

    (let [c-stopped
          (-> c-started
              (component/stop)
              (component/stop)
              (component/stop))]

      (is (pool/closed? c-stopped)))))


(deftest test-pool-with-open
  (with-open [pool (pool/make-pool *CONFIG*)]
    (pool/with-connection [conn pool]
      (let [res (pg/execute conn "select 1 as one")]
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

    (with-open [pool (pool/make-pool *CONFIG* pool-config)]

      (pool/with-connection [conn pool]
        (deliver id1 (pg/id conn)))

      (pool/with-connection [conn pool]
        (pg/terminate conn)
        (deliver id2 (pg/id conn)))

      (pool/with-connection [conn pool]
        (deliver id3 (pg/id conn))
        (let [res (pg/execute conn "select 1 as one")]
          (is (= [{:one 1}] res))))

      (pool/with-connection [conn pool]
        (deliver id4 (pg/id conn)))

      (is (= @id1 @id2))
      (is (not= @id2 @id3))
      (is (= @id3 @id4)))))


(deftest test-pool-termination

  (pool/with-pool [pool *CONFIG*]
    (pool/terminate pool)

    (try
      (pool/with-connection [conn pool]
        42)
      (is false)
      (catch Exception e
        (is (= "the pool has been closed" (ex-message e)))))

    (pool/initiate pool)

    (pool/with-connection [conn pool]
      (let [res (pg/execute conn "select 1 as one")]
        (is (= [{:one 1}] res))))))
