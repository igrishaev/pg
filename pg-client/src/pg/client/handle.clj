(ns pg.client.handle
  (:import
   pg.client.result.Result
   pg.client.connection.Connection)
  (:require
   [pg.error :as e]
   [pg.client.connection :as connection]))


(defmulti -handle
  (fn [^Result result message]
    (:tag message)))


(defmethod -handle :default
  [^Result result message]
  (e/error! "Cannot handle a message: %s" message))


(defmethod -handle :AuthenticationOk
  [^Result result _]
  result)


(defmethod -handle :ParameterStatus
  [^Result result message]

  (let [{:keys [^Connection connection]}
        result

        {:keys [param value]}
        message]

    (connection/set-parameter connection param value))

  result)


(defmethod -handle :BackendKeyData
  [^Result result message]

  (let [{:keys [^Connection connection]}
        result

        {:keys [pid secret-key]}
        message]

    (connection/set-pid connection pid)
    (connection/set-secret-key connection pid))

  result)


(defmethod -handle :ReadyForQuery
  [^Result result message]

  (let [{:keys [^Connection connection]}
        result

        {:keys [tx-status]}
        message]

    (connection/set-tx-status connection tx-status))

  result)
