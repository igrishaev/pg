(ns pg.client.result
  (:import
   java.util.Map
   java.util.HashMap
   java.util.List
   java.util.ArrayList)
  (:require
   [pg.client.coll :as coll]
   [pg.decode.txt :as txt]
   [pg.decode.bin :as bin]
   [pg.client.md5 :as md5]
   [pg.client.conn :as conn]))


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
  ;; TODO: handle
  result)


(defn handle-NotificationResponse
  ;; TODO: handle
  [result conn message]
  result)


(defn handle-NegotiateProtocolVersion
  ;; TODO: throw
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

    (conn/send-password conn hashed))

  result)


(defn handle-AuthenticationCleartextPassword
  [result conn message]
  (let [password
        (conn/get-password conn)]
    (conn/send-password conn password))
  result)


(defn handle-Exception
  [{:as result :keys [^List exceptions]} e]
  (.add exceptions e)
  result)


(defn make-subresult
  [result
   {:as RowDescription :keys [columns]}]

  (let [fn-column
        (get result :fn-column keyword)]

    {:RowDescription RowDescription
     :Rows! (transient [])
     :Keys (coll/for-vec [c columns]
             (-> c :name fn-column))}))


(defn result-add-DataRow [result conn DataRow]

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

    (update result :Rows! conj! Row)))


(defn execute-RowDescription
  [result RowDescription]
  (let [subresult
        (make-subresult result RowDescription)]
    (assoc result :Execute subresult)))


(defn execute-DataRow
  [result conn DataRow]
  (update result
          :Execute
          result-add-DataRow
          conn
          DataRow))


(defn query-RowDescription
  [{:as result :keys [I]}
   RowDescription]

  (let [I+ (inc I)

        subresult
        (make-subresult result RowDescription)]

    (-> result
        (assoc :I I+)
        (assoc-in [:Query I+] subresult))))


(defn query-DataRow
  [{:as result :keys [I]}
   conn
   DataRow]

  (update-in result
             [:Query I]
             result-add-DataRow
             conn
             DataRow))


(defn query-CommandComplete
  [{:as result :keys [I]}
   CommandComplete]
  (-> result
      (assoc-in [:Query I :CommandComplete] CommandComplete)))


(defn throw-ErrorResponse
  [ErrorResponse]
  (throw (ex-info "ErrorResponse" {:error ErrorResponse})))


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
    (case phase

      :auth
      (throw-ErrorResponse message)

      ;; else
      (handle-ErrorResponse result message))

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

    :AuthenticationMD5Password
    (handle-AuthenticationMD5Password result conn message)

    :AuthenticationCleartextPassword
    (handle-AuthenticationCleartextPassword result conn message)

    :BackendKeyData
    (handle-BackendKeyData result conn message)

    (case [phase msg]

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
      (query-DataRow result conn message)

      [:query :CommandComplete]
      (query-CommandComplete result message)

      ;; else

      (throw (ex-info "Cannot handle a message"
                      {:phase phase
                       :message message})))))


(defn finalize-query [{:keys [I ^Map Query]}]

  (loop [i 1
         acc! (transient [])]

    (if (> i I)

      (case (count acc!)
        0 nil
        1 (-> acc! persistent! first)
        (-> acc! persistent!))

      (let [subres
            (.get Query i)

            {:keys [Rows!
                    CommandComplete]}
            subres]

        (if Rows!
          (recur (inc i) (conj! acc! (persistent! Rows!)))
          (recur (inc i) acc!))))))


(defn finalize-execute [{:keys [Execute]}]
  (some-> Execute :Rows! persistent!))


(defn finalize-prepare [result]
  (select-keys result [:statement
                       :ParameterDescription
                       :RowDescription]))


(defn finalize-errors! [{:keys [^List errors]}]
  (when-not (.isEmpty errors)
    (let [error (.get errors 0)]
      (throw (ex-info "ErrorResponse" {:error error})))))


(defn finalize-exeptions! [{:keys [^List exceptions]}]
  (when-not (.isEmpty exceptions)
    (let [e (.get exceptions 0)]
      (throw e))))


(defn finalize [{:as result :keys [phase]}]

  (finalize-errors! result)
  (finalize-exeptions! result)

  (case phase

    :prepare
    (finalize-prepare result)

    :execute
    (finalize-execute result)

    :query
    (finalize-query result)

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

          (if (identical? msg :ReadyForQuery)
            result
            (recur result))))))))
