(ns pg.client2.result
  (:require
   [pg.client2.conn :as conn]))


(defn make-result []
  {})


(defn handle-ReadyForQuery
  [result conn {:keys [tx-status]}]
  (conn/set-tx-status conn tx-status)
  result)


(defn handle-BackendKeyData
  [result conn {:keys [pid secret-key]}]
  (conn/set-pid conn pid)
  (conn/set-secret-key conn secret-key)
  result)


(defn handle-ErrorResponse [result conn message]
  ;; TODO
  result
  #_
  (throw (ex-info "ErrorResponse"
                  {:message message})))


(defn handle-ParameterStatus
  [result conn {:keys [param value]}]
  (conn/set-parameter conn param value)
  result)


(defn handle-BackendKeyData
  [result conn {:keys [pid secret-key]}]
  (conn/set-pid conn pid)
  (conn/set-secret-key conn secret-key)
  result)


(defn handle-RowDescription [result conn message]
  result)


(defn handle-DataRow [result conn message]
  result)


(defn handle-CommandComplete [result conn message]
  result)


(defn handle-ParseComplete [result conn message]
  result)


(defn handle-ParameterDescription [result conn message]
  result)


(defn handle-BindComplete [result conn message]
  result)


(defn handle-PortalSuspended [result conn message]
  result)


(defn handle-NoticeResponse [result conn message]
  result)


(defn handle-NoData [result conn message]
  result)


(defn handle [result conn {:as message :keys [msg]}]

  (case msg

    ;; TODO: list of noop

    :ReadyForQuery
    (handle-ReadyForQuery result conn message)

    :NoData
    (handle-NoData result conn message)

    :NoticeResponse
    (handle-NoticeResponse result conn message)

    :BackendKeyData
    (handle-BackendKeyData result conn message)

    (:AuthenticationOk :EmptyQueryResponse :CloseComplete)
    result

    :PortalSuspended
    (handle-PortalSuspended result conn message)

    :ErrorResponse
    (handle-ErrorResponse result conn message)

    :ParameterStatus
    (handle-ParameterStatus result conn message)

    :RowDescription
    (handle-RowDescription result conn message)

    :DataRow
    (handle-DataRow result conn message)

    :CommandComplete
    (handle-CommandComplete result conn message)

    :ParseComplete
    (handle-ParseComplete result conn message)

    :BindComplete
    (handle-BindComplete result conn message)

    :ParameterDescription
    (handle-ParameterDescription result conn message)

    ;; else

    (throw (ex-info "Cannot handle a message"
                    {:message message}))))


(defn interact [conn until]

  (loop [result (make-result)]

    (let [{:as message :keys [msg]}
          (conn/read-message conn)]

      (let [result
            (handle result conn message)]

        (if (contains? until msg)
          result
          (recur result))))))
