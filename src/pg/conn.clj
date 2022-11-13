(ns pg.conn
  (:require
   [pg.types :as types]
   [pg.const :as const]
   [pg.codec :as codec]
   [pg.auth.scram-sha-256 :as sha-256]
   [pg.bb :as bb]
   [pg.msg :as msg])
  (:import
   java.net.InetSocketAddress
   java.nio.channels.SocketChannel))


(defn send-bb [^SocketChannel ch bb]
  (.write ch (bb/rewind bb)))


(defn auth-pipeline
  [{:as state :keys [user
                     password
                     database
                     ^SocketChannel ch]}]

  (let [bb
        (msg/make-startup database user const/PROT-VER-14)]

    (send-bb ch bb)

    (loop [state-auth nil]

      (let [{:as msg :keys [type]}
            (msg/read-message ch)]

        (case type

          :AuthenticationOk
          (assoc state :state-auth state-auth)

          :AuthenticationSASLContinue
          (let [{:keys [server-first-message]}
                msg

                state-auth
                (-> state-auth
                    (sha-256/step2-server-first-message server-first-message)
                    (sha-256/step3-client-final-message))

                {:keys [client-final-message]}
                state-auth

                bb
                (msg/make-sasl-response client-final-message)]

            (send-bb ch bb)
            (recur state-auth))

          :AuthenticationSASL
          (let [{:keys [sasl-types]}
                msg]

            (cond

              (contains? sasl-types const/SCRAM-SHA-256)
              (let [state-auth
                    (sha-256/step1-client-first-message user password)

                    {:keys [client-first-message]}
                    state-auth

                    bb
                    (msg/make-sasl-init-response const/SCRAM-SHA-256
                                                 client-first-message)]

                (send-bb ch bb)
                (recur state-auth))

              :else
              (throw (ex-info "Other SCRAM algorithms are not implemented yet"
                              {:sasl-types sasl-types}))))

          :AuthenticationSASLFinal
          (let [{:keys [server-final-message]}
                msg

                state-auth
                (-> state-auth
                    (sha-256/step4-server-final-message server-final-message)
                    (sha-256/step5-verify-server-signatures))]
            (recur state-auth))

          :AuthenticationCleartextPassword
          (let [bb (msg/make-clear-text-password password)]
            (send-bb ch bb)
            (recur nil))

          :AuthenticationMD5Password
          (let [{:keys [salt]} msg
                bb (msg/make-md5-password user password salt)]
            (send-bb ch bb)
            (recur nil))

          :ErrorResponse
          (let [{:keys [errors]} msg]
            (throw (ex-info "Authentication failed"
                            {:errors errors})))

          ;; else
          (throw (ex-info "Unhandled message in the auth pipeline"
                          {:msg msg})))))))


(defn init-pipeline
  [{:as state :keys [^SocketChannel ch]}]

  (loop [state state]

    (let [{:as msg :keys [type]}
          (msg/read-message ch)]

      (case type

        :ParameterStatus
        (let [{:keys [param value]} msg]
          (recur (assoc-in state [:params param] value)))

        :BackendKeyData
        (let [{:keys [pid secret-key]} msg]
          (recur (assoc state
                        :pid pid
                        :secret-key secret-key)))

        :ReadyForQuery
        (let [{:keys [tx-status]} msg]
          (case tx-status
            \E
            (throw (ex-info "Transaction is in the error state"
                            {:msg msg}))
            ;; else
            (assoc state :tx-status tx-status)))

        ;; else
        (throw (ex-info "Unhandled message in the init pipeline"
                        {:msg msg}))))))


(defn data-pipeline
  [{:as state :keys [^SocketChannel ch]}]

  (loop [query-fields nil
         query-result (transient [])
         errors nil]

    (let [{:as msg :keys [type]}
          (msg/read-message ch)]

      (println msg)

      (case type

        :ParseComplete
        (recur query-fields
               query-result
               errors)

        :ErrorResponse
        (let [{:keys [errors]} msg]
          (recur query-fields
                 query-result
                 errors))

        :RowDescription
        (let [{:keys [fields]}
              msg]

          (recur fields
                 query-result
                 nil))

        :DataRow
        (let [{:keys [columns]}
              msg

              field-count
              (count columns)

              row
              (loop [i 0
                     row (transient {})]

                (if (= i field-count)
                  (persistent! row)

                  (let [column
                        (get columns i)

                        column-meta
                        (get query-fields i)

                        {field-name :name}
                        column-meta

                        value
                        (types/parse-column column column-meta)]

                    (recur (inc i)
                           (assoc! row field-name value)))))]

          (recur query-fields
                 (conj! query-result row)
                 nil))

        :CommandComplete
        (recur query-fields
               query-result
               nil)

        :ReadyForQuery
        (let [{:keys [tx-status]} msg]

          (cond

            (= tx-status \E)
            (throw (ex-info "Transaction failed"
                            {:msg msg}))

            errors
            (throw (ex-info "Error response"
                            {:errors errors}))

            :else
            (persistent! query-result)
            #_
            (recur query-fields
                   query-result
                   errors)))

        ;; else
        (throw (ex-info "Unhandled message in the data pipeline"
                        {:msg msg}))))))


(defn connect [{:as state :keys [^String host
                                 ^Integer port]}]

  (let [addr
        (new java.net.InetSocketAddress host port)

        ch
        (SocketChannel/open addr)]

    (-> state
        (assoc :ch ch
               :addr addr)
        (auth-pipeline)
        (init-pipeline))))


(defmacro with-lock
  [state & body]
  `(locking (:o ~state)
     ~@body))


(defn query
  [{:as state :keys [ch]} sql]
  (with-lock state
    (send-bb ch (msg/make-query sql))
    (data-pipeline state)))


(defn sync [{:as state :keys [ch]}]
  (with-lock state
    (send-bb ch (msg/make-sync))))


(defn flush [{:as state :keys [ch]}]
  (with-lock state
    (send-bb ch (msg/make-flush))))


(defn make-state [state]
  (-> state
      (assoc :o (new Object))))


(comment

  (def -state
    (-> {:host "127.0.0.1"
         :port 15432
         :user "ivan"
         :database "ivan"
         :password "secret"}
        make-state
        connect))

  (send-bb
   (:ch -state)
   (msg/make-parse "st3"

                      "
select
1 as foo,
'hello' as bar,
true as aaa,
1.1::float as fl,
1.1::float4 as fl1,
1.1::float8 as fl2,
1.2 as fl3,
NULL as nil,
now() as date,
'{1, 2, 3}'::int2vector[] as intvec

-- '{1, 2, 3}'::int2[] as arr1
"

                   #_
                   "select * from foo where id = $1"

                   #_
                   [20]))

  (send-bb
   (:ch -state)
   (msg/make-function-call 1299 0 [] 0))

  (send-bb
   (:ch -state)
   (msg/make-describe-statement "st3"))

  (send-bb
   (:ch -state)
   (msg/make-bind
    "pt3"
    "st3"
    [#_[0 (.getBytes "1")]]
    [1 1 1 1 1 1 1 1 1 1]
    ))

  (send-bb
   (:ch -state)
   (msg/make-describe-portal "pt3"))

  (send-bb
   (:ch -state)
   (msg/make-execute
    "pt3" 999))

  (sync -state)
  (flush -state)

  (msg/read-message (:ch -state))




  (query "")

  (query
   -state
   "
select
1 as foo,
'hello' as bar,
true as aaa,
1.1::float as fl,
1.1::float4 as fl1,
1.1::float8 as fl2,
1.2 as fl3,
NULL as nil,
now() as date,
'{1, 2, 3}'::int2vector[] as intvec

-- '{1, 2, 3}'::int2[] as arr1
"
   )




#_
[{:index 0, :name foo, :table-id 0, :column-id 0, :type-id 23, :type-size 4, :type-mod-id -1, :format 0}
 {:index 1, :name bar, :table-id 0, :column-id 0, :type-id 25, :type-size -1, :type-mod-id -1, :format 0}
 {:index 2, :name aaa, :table-id 0, :column-id 0, :type-id 16, :type-size 1, :type-mod-id -1, :format 0}
 {:index 3, :name fl, :table-id 0, :column-id 0, :type-id 1700, :type-size -1, :type-mod-id -1, :format 0}
 {:index 4, :name nil, :table-id 0, :column-id 0, :type-id 25, :type-size -1, :type-mod-id -1, :format 0}
 {:index 5, :name date, :table-id 0, :column-id 0, :type-id 1184, :type-size 8, :type-mod-id -1, :format 0}]




  )
