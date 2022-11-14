(ns pg.conn
  (:require
   ;; [pg.types :as types]
   ;; [pg.const :as const]
   ;; [pg.codec :as codec]
   ;; [pg.auth.scram-sha-256 :as sha-256]
   [pg.bb :as bb]
   [pg.msg :as msg]
   )
  (:import
   java.net.InetSocketAddress
   java.nio.channels.SocketChannel))


(defn write-bb
  [{:keys [^SocketChannel ch]} bb]
  (.write ch (bb/rewind bb)))


(defn read-bb [{:keys [^SocketChannel ch]}]
  (msg/read-message ch))


(defmacro with-lock
  [state & body]
  `(locking (:o ~state)
     ~@body))


(defn connect [{:as conn :keys [^String host
                                ^Integer port]}]

  (let [addr
        (new java.net.InetSocketAddress host port)

        ch
        (SocketChannel/open addr)]

    (assoc conn
           :o (new Object)
           :ch ch
           :addr addr)))


#_
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


#_
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




#_
(defn query
  [conn sql]
  (with-lock conn
    (write-bb conn (msg/make-query sql))
    (data-pipeline conn)))


#_
(defn sync [state]
  (with-lock state
    (write-bb state (msg/make-sync))))


#_
(defn flush [{:as state :keys [ch]}]
  (with-lock state
    (write-bb ch (msg/make-flush))))



(comment

  #_
  (def -conn
    (connect
     {:host "127.0.0.1"
      :port 15432
      :user "ivan"
      :database "ivan"
      :password "secret"}))

  (write-bb
   -conn
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

  (write-bb
   -conn
   (msg/make-function-call 1299 0 [] 0))

  (write-bb
   -conn
   (msg/make-describe-connment "st3"))

  (write-bb
   -conn
   (msg/make-bind
    "pt3"
    "st3"
    [#_[0 (.getBytes "1")]]
    [1 1 1 1 1 1 1 1 1 1]
    ))

  (write-bb
   -conn
   (msg/make-describe-portal "pt3"))

  (write-bb
   -conn
   (msg/make-execute
    "pt3" 999))

  (sync -conn)
  (flush -conn)

  (msg/read-message (:ch -conn))

  (query "")

  (query
   -conn
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
