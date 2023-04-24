(ns pg.pipeline
  (:require
   [pg.error :as e]
   [pg.types.decode :as decode]
   [pg.conn :as conn]
   [pg.codec :as codec]
   [pg.const :as const]
   [pg.auth.scram-sha-256 :as sha-256]
   [pg.auth.md5 :as md5]
   [pg.msg :as msg]))


(defn decode-row
  [DataRow RowDescription enc]

  (let [{:keys [fields
                field-count]}
        RowDescription

        {:keys [columns]}
        DataRow]

    (loop [i 0
           acc! (transient {})]

      (if (= i field-count)
        (persistent! acc!)

        (let [column
              (get columns i)

              field
              (get fields i)

              cname
              (-> field
                  :name
                  (codec/bytes->str enc)
                  keyword)

              cvalue
              (decode/decode column field enc)]

          (recur (inc i)
                 (assoc! acc! cname cvalue)))))))


(defn handle-auth-sasl
  [{:as conn :keys [user password]}
   state
   {:keys [sasl-types]}]

  (cond

    (contains? sasl-types const/SCRAM-SHA-256)
    (let [auth
          (sha-256/step1-client-first-message user password)

          {:keys [client-first-message]}
          auth

          bb
          (msg/make-sasl-init-response const/SCRAM-SHA-256
                                       client-first-message)]

      (conn/write-bb conn bb)
      (assoc state :auth auth))

    ;; special case
    :else
    (assoc state
           :end? true
           :exception
           (e/error "None of the server SCRAM algorithms is supported"
                    {:sasl-types sasl-types}))))


(defn handle-param-status [conn state {:keys [param value]}]
  (conn/param conn param value)
  state)


(defn handle-notification-response [conn state msg]
  (let [enc
        (conn/server-encoding conn)

        {:keys [fn-notification-handler]}
        conn

        {:keys [pid]}
        msg

        channel
        (-> msg
            :channel
            (codec/bytes->str enc))

        message
        (-> msg
            :message
            (codec/bytes->str enc))]

    (when fn-notification-handler
      (fn-notification-handler conn pid channel message))

    state))


(defn handle-error-response
  [conn
   {:as state :keys [phase]}
   msg]
  (assoc state
         :end? (= phase :auth)
         :ErrorResponse msg))


(defn handle-ready-for-query
  [conn
   state
   {:as msg :keys [tx-status]}]
  (conn/tx-status conn tx-status)
  (assoc state
         :end? true
         :ReadyForQuery msg))


(defn handle-backend-data
  [conn
   state
   {:keys [pid secret-key]}]
  (doto conn
    (conn/param :pid pid)
    (conn/param :secret-key secret-key))
  state)


(defn handle-row-description
  [conn state msg]
  (assoc state
         :Rows! (transient [])
         :RowDescription msg))


(defn handle-data-row [conn state msg]
  (let [enc
        (conn/server-encoding conn)

        {:keys [RowDescription]}
        state

        Row
        (decode-row msg RowDescription enc)]

    (update state :Rows! conj! Row)))


(defn handle-notice-response [conn state msg]
  (let [enc
        (conn/server-encoding conn)

        {:keys [fn-notice-handler]}
        conn

        {:keys [messages]}
        msg]

    (when fn-notice-handler
      (let [decoded
            (loop [[{:as head :keys [type message]} & tail] messages
                   acc []]
              (if head
                (let [node
                      {:type (char type)
                       :message (codec/bytes->str message enc)}]
                  (recur tail (conj acc node)))
                acc))]
        (fn-notice-handler conn decoded)))

    state))


(defn handle-auth-ok [conn state msg]
  (assoc state :phase :data))


(defn handle-auth-sasl-continue
  [conn
   {:as state :keys [auth]}
   msg]

  (let [{:keys [server-first-message]}
        msg

        auth
        (-> auth
            (sha-256/step2-server-first-message server-first-message)
            (sha-256/step3-client-final-message))

        {:keys [client-final-message]}
        auth

        bb
        (msg/make-sasl-response client-final-message)]

    (conn/write-bb conn bb)
    (assoc state :auth auth)))


(defn handle-auth-sasl-final
  [conn
   {:as state :keys [auth]}
   {:keys [server-final-message]}]

  (let [auth
        (-> auth
            (sha-256/step4-server-final-message server-final-message)
            (sha-256/step5-verify-server-signatures))]

    (assoc state :auth auth)))


(defn handle-auth-cleartext-pass
  [{:as conn :keys [password]}
   state
   msg]
  (let [bb (msg/make-clear-text-password password)]
    (conn/write-bb conn bb))
  state)


(defn handle-auth-md5-pass
  [{:as conn :keys [user password]}
   state
   msg]
  (let [{:keys [salt]}
        msg

        hashed-pass
        (md5/hash-password user password salt)

        bb
        (msg/make-md5-password
         (codec/str->bytes hashed-pass))]

    (conn/write-bb conn bb))
  state)


(defn handle-param-description
  [conn state msg]
  (assoc state :ParameterDescription msg))


(defn handle-parse-complete [conn state msg]
  (assoc state :ParseComplete msg))


(defn process-message
  [conn state {:as msg :keys [type]}]

  (println msg)

  (case type

    :ReadyForQuery
    (handle-ready-for-query conn state msg)

    :ErrorResponse
    (handle-error-response conn state msg)

    ;; :NegotiateProtocolVersion
    ;; :AuthenticationKerberosV5
    ;; :AuthenticationSCMCredential
    ;; :AuthenticationGSS
    ;; :AuthenticationGSSContinue
    ;; :AuthenticationSSPI

    :AuthenticationOk
    (handle-auth-ok conn state msg)

    :AuthenticationSASL
    (handle-auth-sasl conn state msg)

    :AuthenticationSASLContinue
    (handle-auth-sasl-continue conn state msg)

    :AuthenticationSASLFinal
    (handle-auth-sasl-final conn state msg)

    :AuthenticationCleartextPassword
    (handle-auth-cleartext-pass conn state msg)

    :AuthenticationMD5Password
    (handle-auth-md5-pass conn state msg)

    :ParameterStatus
    (handle-param-status conn state msg)

    :NotificationResponse
    (handle-notification-response conn state msg)

    :NoticeResponse
    (handle-notice-response conn state msg)

    :ParameterDescription
    (handle-param-description conn state msg)

    :BackendKeyData
    (handle-backend-data conn state msg)

    :ParseComplete
    (handle-parse-complete conn state msg)

    (:CloseComplete :BindComplete :NoData)
    state

    :RowDescription
    (handle-row-description conn state msg)

    :DataRow
    (handle-data-row conn state msg)

    :CommandComplete
    (assoc state :CommandComplete msg)

    ;; else
    (e/error! "Unhandled message in the pipeline"
              {:msg msg
               :in ::here})))


(defn state->result
  [conn state]

  (let [enc
        (conn/server-encoding conn)

        {:keys [Rows!
                ErrorResponse
                ReadyForQuery
                RowDescription
                ParameterDescription
                ParseComplete]}
        state

        {:keys [errors]}
        ErrorResponse

        {:keys [tx-status]}
        ReadyForQuery]

    (cond

      (= tx-status const/TX-ERROR)
      (e/error! "Transaction is in the error state")

      errors
      (let [message
            (with-out-str
              (doseq [{:keys [label bytes]}
                      errors]
                (println " -" label (codec/bytes->str bytes enc))))]
        (e/error! message))

      ParseComplete
      {:RowDescription RowDescription
       :ParameterDescription ParameterDescription}

      Rows!
      (persistent! Rows!)


      :else
      nil
      ;; state

      )))


(defn pipeline

  ([conn]
   (pipeline conn nil))

  ([conn state]

   (loop [state state]

     (let [{:as msg :keys [type]}
           (conn/read-bb conn)

           [state* e]
           (e/with-pcall
             (process-message conn state msg))

           {:keys [end? exception]}
           state*]

       (cond

         e
         (recur (assoc state :exception e))

         ;; TODO: fix
         end?
         (if exception
           (let [bb (msg/make-terminate)]
             #_
             (conn/write-bb conn bb)
             (throw exception))
           (state->result conn state*))

         :else
         (recur state*))))))
