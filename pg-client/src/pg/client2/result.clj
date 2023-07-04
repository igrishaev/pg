(ns pg.client2.result
  (:import
   java.util.List
   java.util.ArrayList)
  (:require
   [pg.client2.md5 :as md5]
   [pg.client2.conn :as conn]))


(defn make-result [phase]
  {:phase phase
   :errors (new ArrayList)})


(defn handle-ReadyForQuery
  [result conn {:keys [tx-status]}]
  (conn/set-tx-status conn tx-status)
  result)


(defn handle-BackendKeyData
  [result conn {:keys [pid secret-key]}]
  (conn/set-pid conn pid)
  (conn/set-secret-key conn secret-key)
  result)


(defn handle-ErrorResponse
  [{:as result :keys [^List errors]}
   conn
   message]
  (.add errors message)
  result)


(defn handle-ParameterStatus
  [result conn {:keys [param value]}]
  (conn/set-parameter conn param value)
  result)


(defn handle-BackendKeyData
  [result conn {:keys [pid secret-key]}]
  (conn/set-pid conn pid)
  (conn/set-secret-key conn secret-key)
  result)


(defn handle-NoticeResponse [result conn message]
  result)


(defn handle-NotificationResponse
  [result conn message]
  result)


(defn handle-NegotiateProtocolVersion
  [result conn message]
  result)


(defn handle-AuthenticationMD5Password
  [result conn {:keys [salt]}]

  (let [user
        (conn/get-user conn)

        password
        (conn/get-password conn)

        hashed
        (md5/hash-password user password salt)]

    (conn/send-password hashed))

  result)


(defn handle [{:as result :keys [phase]}
              conn
              {:as message :keys [msg]}]

  (case msg

    (:AuthenticationOk
     :EmptyQueryResponse
     :CloseComplete
     :BindComplete
     :NoData
     :ParseComplete
     :PortalSuspended)
    result

    :ErrorResponse
    (handle-ErrorResponse result conn message)

    :ReadyForQuery
    (handle-ReadyForQuery result conn message)

    :ParameterStatus
    (handle-ParameterStatus result conn message)

    :NoticeResponse
    (handle-NoticeResponse result conn message)

    :NotificationResponse
    (handle-NotificationResponse result conn message)

    :NegotiateProtocolVersion
    (handle-NegotiateProtocolVersion result conn message)

    (case [phase msg]

      ;;
      ;; auth
      ;;

      [:auth :AuthenticationMD5Password]
      (handle-AuthenticationMD5Password result conn message)

      [:auth :BackendKeyData]
      (handle-BackendKeyData result conn message)

      ;;
      ;; prepare
      ;;

      [:prepare :ParameterDescription]
      (assoc result :ParameterDescription message)

      [:prepare :RowDescription]
      (assoc result :RowDescription message)

      ;;
      ;; execute
      ;;

      [:execute :RowDescription]
      (assoc result
             :RowDescription message
             :Rows (transient []))

      [:execute :DataRow]
      (update result :Rows conj! message)

      [:execute :CommandComplete]
      (assoc result :CommandComplete message)

      ;;
      ;; query
      ;;

      ;; else

      (throw (ex-info "Cannot handle a message"
                      {:phase phase
                       :message message})))))


(defn finalize [{:as result :keys [phase]}]

  (case phase

    :prepare
    (select-keys result [:statement
                         :ParameterDescription
                         :RowDescription])

    :execute
    (some-> result :Rows persistent!)

    ;; else

    result))


(defn interact [conn until phase]

  (finalize

   (loop [result (make-result phase)]

     (let [{:as message :keys [msg]}
           (conn/read-message conn)]

       (let [result
             (handle result conn message)]

         (if (contains? until msg)
           result
           (recur result)))))))
