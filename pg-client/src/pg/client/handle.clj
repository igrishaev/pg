(ns pg.client.handle
  (:import
   pg.client.connection.Connection
   [pg.client.message
    AuthenticationOk
    DataRow
    CommandComplete
    RowDescription
    ParameterStatus
    BackendKeyData
    ReadyForQuery]
   pg.client.result.Result)
  (:require
   [pg.client.result :as result]
   [pg.client.connection :as connection]
   [pg.error :as e]))


(defmulti -handle
  (fn [^Result result message]
    (type message)))


(defmethod -handle :default
  [^Result result message]
  (e/error! "Cannot handle a message: %s" message))


(defmethod -handle AuthenticationOk
  [^Result result _]
  result)


(defmethod -handle ParameterStatus
  [^Result result message]

  (let [{:keys [^Connection connection]}
        result

        {:keys [param value]}
        message]

    (connection/set-parameter connection param value))

  result)


(defmethod -handle BackendKeyData
  [^Result result message]

  (let [{:keys [^Connection connection]}
        result

        {:keys [pid secret-key]}
        message]

    (connection/set-pid connection pid)
    (connection/set-secret-key connection secret-key))

  result)


(defmethod -handle ReadyForQuery
  [^Result result message]

  (let [{:keys [^Connection connection]}
        result

        {:keys [tx-status]}
        message]

    (connection/set-tx-status connection tx-status))

  (reduced result))


(defmethod -handle RowDescription
  [^Result result message]
  (result/add-RowDescription result message))


(defmethod -handle DataRow
  [^Result result message]
  (result/add-DataRow result message))


(defmethod -handle CommandComplete
  [^Result result message]
  (result/add-CommandComplete result message))


(defn handle [^Result result messages]
  (reduce -handle result messages))
