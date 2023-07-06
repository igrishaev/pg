(ns pg.client2.result
  (:import
   java.util.Map
   java.util.HashMap
   java.util.List
   java.util.ArrayList)
  (:require
   [pg.client2.coll :as coll]
   [pg.decode.txt :as txt]
   [pg.decode.bin :as bin]
   [pg.client2.md5 :as md5]
   [pg.client2.conn :as conn]))


(defn make-result [phase init]
  (assoc init
         :I 0
         :Query {}
         :phase phase
         :errors (new ArrayList)
         :exceptions (new ArrayList)))


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


(defn handle-Exception
  [{:as result :keys [^List exceptions]} e]
  (.add exceptions e)
  result)


(defn execute-RowDescription
  [result
   {:as message :keys [columns]}]
  (assoc result
         :Keys (mapv :name columns)
         :RowDescription message
         :Rows (transient [])))


(defn execute-DataRow
  [result conn DataRow]

  (let [encoding
        (conn/get-server-encoding conn)

        {:keys [^List Keys
                RowDescription]}
        result

        {:keys [^List values]}
        DataRow

        {:keys [^List columns]}
        RowDescription

        ;; TODO: fill opt
        opt
        {}

        values-decoded
        (coll/doN [i (count values)]

          (let [col
                (.get columns i)

                {:keys [type-oid
                        format]}
                col

                ^bytes buf
                (.get values i)]

            (case (int format)
              0 (let [string (new String buf encoding)]
                  (txt/decode string type-oid opt))
              1 (bin/decode buf type-oid opt))))

        Row
        (zipmap Keys values-decoded)]

    (update result :Rows conj! Row)))


(defn query-RowDescription
  [{:as result :keys [I]}
   RowDescription]
  (let [I+ (inc I)]
    (-> result
        (assoc :I I+)
        (assoc-in [:Query I+] {:RowDescription RowDescription
                               :Rows (transient [])}))))


(defn query-DataRow
  [{:as result :keys [I]}
   DataRow]
  (update-in result [:Query I :Rows] conj! DataRow))


(defn query-CommandComplete
  [{:as result :keys [I]}
   CommandComplete]
  (-> result
      (assoc-in [:Query I :CommandComplete] CommandComplete)
      (update-in [:Query I :Rows] persistent!)))


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
      (execute-RowDescription result message)

      [:execute :DataRow]
      (execute-DataRow result conn message)

      [:execute :CommandComplete]
      (assoc result :CommandComplete message)

      ;;
      ;; close statement
      ;;

      [:close-statement :CommandComplete]
      result

      ;;
      ;; query
      ;;

      [:query :RowDescription]
      (query-RowDescription result message)

      [:query :DataRow]
      (query-DataRow result message)

      [:query :CommandComplete]
      (query-CommandComplete result message)

      ;; else

      (throw (ex-info "Cannot handle a message"
                      {:phase phase
                       :message message})))))


(defn finalize [{:as result :keys [phase
                                   ^List errors
                                   ^List exceptions]}]

  (when-not (.isEmpty errors)
    (let [error (.get errors 0)]
      (throw (ex-info "ErrorResponse" {:error error}))))

  (when-not (.isEmpty exceptions)
    (let [e (.get exceptions 0)]
      (throw e)))

  (case phase

    :prepare
    (select-keys result [:statement
                         :ParameterDescription
                         :RowDescription])

    :execute
    (some-> result :Rows persistent!)

    :query
    (:Query result)

    ;; else

    result))


(defn interact

  ([conn phase]
   (interact conn phase nil))

  ([conn phase init]

   (finalize

    (loop [result (make-result phase init)]

      (let [{:as message :keys [msg]}
            (conn/read-message conn)]

        (let [result
              (try
                (handle result conn message)
                (catch Throwable e
                  (handle-Exception result e)))]

          (if (or (identical? msg :ReadyForQuery)
                  (identical? msg :ErrorResponse))
            result
            (recur result))))))))
