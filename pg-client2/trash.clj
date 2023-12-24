
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


      (catch Exception e
        (is (= "ErrorResponse" (ex-message e)))
        (is (= {:error
                {:msg :ErrorResponse
                 :errors
                 {:severity "ERROR"
                  :verbosity "ERROR"
                  :code "42601"
                  :message "syntax error at or near \"selekt\""
                  :position "1"
                  :function "scanner_yyerror"}}}
               (-> e
                   (ex-data)
                   (update-in [:error :errors]
                              dissoc :file :line)))))
