(ns pg.client2.result
  (:require
   [pg.client2.conn :as conn]))


(defn make-result []
  {})


(defn handle-ReadyForQuery
  [conn result {:keys [tx-status]}]
  (conn/set-tx-status conn tx-status)
  result)


(defn handle-BackendKeyData
  [conn result {:keys [pid secret-key]}]
  (conn/set-pid conn pid)
  (conn/set-secret-key conn secret-key)
  result)


(defn handle-ErrorResponse [conn result message]
  (throw (ex-info "ErrorResponse"
                  {:message message})))


(defn handle-ParameterStatus
  [conn result {:keys [param value]}]
  (conn/set-parameter conn param value)
  result)


(defn handle-BackendKeyData
  [conn result {:keys [pid secret-key]}]
  (conn/set-pid conn pid)
  (conn/set-secret-key conn secret-key)
  result)


(defn handle-RowDescription [conn result message]
  result)


(defn handle-DataRow [conn result message]
  result)


(defn handle-CommandComplete [conn result message]
  result)


(defn handle-ParseComplete [conn result message]
  result)


(defn handle-ParameterDescription [conn result message]
  result)


(defn handle-BindComplete [conn result message]
  result)


(defn handle-PortalSuspended [conn result message]
  result)


(defn handle-NoticeResponse [conn result message]
  result)


(defn handle-NoData [conn result message]
  result)


(defn handle [conn result {:as message :keys [msg]}]

  (case msg

    ;; TODO: list of noop

    :ReadyForQuery
    (handle-ReadyForQuery conn result message)

    :NoData
    (handle-NoData conn result message)

    :NoticeResponse
    (handle-NoticeResponse conn result message)

    :BackendKeyData
    (handle-BackendKeyData conn result message)

    (:AuthenticationOk :EmptyQueryResponse)
    result

    :PortalSuspended
    (handle-PortalSuspended conn result message)

    :ErrorResponse
    (handle-ErrorResponse conn result message)

    :ParameterStatus
    (handle-ParameterStatus conn result message)

    :RowDescription
    (handle-RowDescription conn result message)

    :DataRow
    (handle-DataRow conn result message)

    :CommandComplete
    (handle-CommandComplete conn result message)

    :ParseComplete
    (handle-ParseComplete conn result message)

    :BindComplete
    (handle-BindComplete conn result message)

    :ParameterDescription
    (handle-ParameterDescription conn result message)

    ;; else

    (throw (ex-info "Cannot handle a message"
                    {:message message}))))


(defn interact [conn until]

  (loop [result (make-result)]

    (let [{:as message :keys [msg]}
          (conn/read-message conn)]

      (let [result
            (handle conn result message)]

        (if (contains? until msg)
          result
          (recur result))))))
