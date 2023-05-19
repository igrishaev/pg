(ns pg.client.handle
  (:require
   pg.client.message)
  (:import
   [pg.client.message
    AuthenticationMD5Password
    AuthenticationOk
    DataRow
    ErrorResponse
    CommandComplete
    RowDescription
    ParameterStatus
    BackendKeyData
    ReadyForQuery])
  (:require
   [pg.client.proto.connection :as connection]
   [pg.client.proto.result :as result]
   [pg.error :as e]))


(defmulti -handle
  (fn [result message]
    (type message)))


(defmethod -handle :default
  [result message]
  (e/error! "Cannot handle a message: %s" message))


(defmethod -handle AuthenticationOk
  [result _]
  result)


(defmethod -handle ParameterStatus
  [result message]

  (let [connection
        (result/get-connection result)

        {:keys [param value]}
        message]

    (connection/set-parameter connection param value))

  result)


(defmethod -handle BackendKeyData
  [result message]

  (let [connection
        (result/get-connection result)

        {:keys [pid secret-key]}
        message]

    (connection/set-pid connection pid)
    (connection/set-secret-key connection secret-key))

  result)


(defmethod -handle ReadyForQuery
  [result message]

  (let [connection
        (result/get-connection result)

        {:keys [tx-status]}
        message]

    (connection/set-tx-status connection tx-status))

  result)


(defmethod -handle RowDescription
  [result message]
  (result/add-RowDescription result message))


(defmethod -handle DataRow
  [result message]
  (result/add-DataRow result message))


(defmethod -handle CommandComplete
  [result message]
  (result/add-CommandComplete result message))


(defmethod -handle ErrorResponse
  [result message]
  (result/add-ErrorResponse result message))


(defn handle [result messages]
  (-> (reduce -handle result messages)
      (result/complete)))
