(ns pg.pipeline
  (:require
   [pg.error :as e]
   [pg.types :as types]
   [pg.conn :as conn]
   [pg.codec :as codec]
   [pg.const :as const]
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
              (types/parse-column column field enc)]

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

    ;; TODO terminate?

    :else
    (assoc state
           :end? true
           :error "No other SCRAM algorithms have been implemented yet")))


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


(defn handle-ready-for-query [conn state msg]
  (let [{:keys [tx-status]}
        msg]

    (assoc state :end? true)))


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
                      {:type type
                       :message (codec/bytes->str message enc)}]
                  (recur (conj acc node) tail))
                acc))]
        (fn-notice-handler conn decoded)))

    conn))


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


(defn process-message
  [conn state {:as msg :keys [type]}]

  (println msg)

  (case type

    :ReadyForQuery
    (handle-ready-for-query conn state msg)

    :ErrorResponse
    (handle-error-response conn state msg)

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
    1
    #_
    (let [bb (msg/make-clear-text-password password)]
            (conn/write-bb conn bb)
            (recur conn auth))

    :AuthenticationMD5Password
    1
    #_
    (let [{:keys [salt]}
                msg

                hashed-pass
                (md5/hash-password user password salt)

                bb
                (msg/make-md5-password
                 (codec/str->bytes hashed-pass))]

            (conn/write-bb conn bb)
            (recur conn auth))


    :ParameterStatus
    (handle-param-status conn state msg)

    :NotificationResponse
    (handle-notification-response conn state msg)

    :NoticeResponse
    (handle-notice-response conn state msg)

    :BackendKeyData
    (handle-backend-data conn state msg)

    (:CloseComplete :ParseComplete)
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


(defn pipeline [conn state]

  (loop [state state]

    (let [{:as msg :keys [type]}
          (conn/read-bb conn)

          [state* e]
          (e/with-pcall
            (process-message conn state msg))]

      (cond

        e
        (recur (update state :exceptions conj e))

        (:end? state*)
        state*

        :else
        (recur state*)))))
