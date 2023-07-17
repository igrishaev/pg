(ns pg.pool-test
  (:require
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


;; test error state
;; test transaction state
