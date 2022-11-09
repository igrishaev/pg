(ns pg.msg
  (:import
   java.nio.channels.SocketChannel)
  (:require
   [pg.codec :as codec]
   [pg.scram :as scram]
   [pg.const :as const]
   [pg.bb :as bb]))


(defn parse-row-description [len bb]

  (let [field-count
        (bb/read-int16 bb)

        fields
        (vec
         (for [i (range field-count)]
           {:index       i
            :name        (bb/read-cstring bb)
            :table-id    (bb/read-int32 bb)
            :column-id   (bb/read-int16 bb)
            :type-id     (bb/read-int32 bb)
            :type-size   (bb/read-int16 bb)
            :type-mod-id (bb/read-int32 bb)
            :format      (bb/read-int16 bb)}))]

    {:type :RowDescription
     :len len
     :field-count field-count
     :fields fields}))


(defn coerce-param-value [value]
  (case value
    "off" false
    "on"  true
    ""    nil
    value))


(defn parse-param-status [len bb]

  (let [param (bb/read-cstring bb)
        value (bb/read-cstring bb)]

    {:type :ParameterStatus
     :len len
     :param param
     :value (coerce-param-value value)}))


(defn parse-auth-response [len bb]

  (let [status (int (bb/read-int32 bb))]

    (case status

      0
      {:type :AuthenticationOk
       :len len
       :status status}

      2
      {:type :AuthenticationKerberosV5
       :len len
       :status status}

      3
      {:type :AuthenticationCleartextPassword
       :len len
       :status status}

      5
      (let [salt (bb/read-bytes bb 4)]
        {:type :AuthenticationMD5Password
         :len len
         :status status
         :salt salt})

      6
      {:type :AuthenticationSCMCredential
       :len len
       :status status}

      7
      {:type :AuthenticationGSS
       :len len
       :status status}

      8
      (let [auth (bb/read-rest bb)]
        {:type :AuthenticationGSSContinue
         :len len
         :status status
         :auth auth})

      9
      {:type :AuthenticationSSPI
       :len len
       :status status}

      10
      (let [auth-types
            (loop [acc []]
              (let [item (bb/read-cstring bb)]
                (if (= item "")
                  acc
                  (recur (conj acc item)))))]
        {:type :AuthenticationSASL
         :len len
         :status status
         :auth-types auth-types})

      11
      (let [message (bb/read-rest bb)]
        {:type :AuthenticationSASLContinue
         :len len
         :status status
         :message message})

      12
      (let [auth (bb/read-rest bb)]
        {:type :AuthenticationSASLFinal
         :len len
         :status status
         :auth auth})

      ;; else
      (throw (ex-info "Unknown auth message"
                      {:status status
                       :bb bb})))))


(defn parse-backend-data [len bb]
  (let [pid (bb/read-int32 bb)
        secret-key (bb/read-int32 bb)]
    {:type :BackendKeyData
     :len len
     :pid pid
     :secret-key secret-key}))


(defn parse-ready-for-query [len bb]
  (let [tx-status (char (bb/read-byte bb))]
    {:type :ReadyForQuery
     :len len
     :tx-status tx-status}))


(defn parse-error-response [len bb]
  (let [errors
        (loop [acc []]
          (let [field-type (bb/read-byte bb)]
            (if (zero? field-type)
              acc
              (let [field-text (bb/read-cstring bb)]
                (recur (conj acc {:type (char field-type)
                                  :text field-text}))))))]
    {:type :ErrorResponse
     :len len
     :errors errors}))


(defn parse-notice-response [len bb]
  (let [messages
        (loop [acc []]
          (let [field-type (bb/read-byte bb)]
            (if (zero? field-type)
              acc
              (let [field-text (bb/read-cstring bb)]
                (recur (conj acc {:type field-type :text field-text}))))))]
    {:type :NoticeResponse
     :len len
     :messages messages}))


(defn parse-bind-complete [len bb]
  {:type :BindComplete
   :len len})


(defn parse-close-complete [len bb]
  {:type :CloseComplete
   :len len})


(defn parse-command-complete [len bb]
  (let [tag (bb/read-cstring bb)]
    {:type :CommandComplete
     :len len
     :tag tag}))


(defn parse-data-row [len bb]
  (let [amount
        (bb/read-int16 bb)

        columns
        (vec
         (for [i (range amount)]
           (let [col-len (bb/read-int32 bb)]
             (when-not (= col-len -1)
               (bb/read-bytes bb col-len)))))]

    {:type :DataRow
     :len len
     :columns columns}))


(defn parse-empty-query-response [len bb]
  {:type :EmptyQueryResponse
   :len len})


(defn parse-function-call-response [len bb]
  (let [res-len (bb/read-int32 bb)
        res-val (bb/read-bytes bb res-len)]
    {:type :FunctionCallResponse
     :len len
     :result res-val}))


(defn parse-negotiate-protocol-version [len bb]
  (let [minor-version
        (bb/read-int32 bb)

        failed-params-count
        (bb/read-int32 bb)

        failed-params
        (vec
         (for [i (range failed-params-count)]
           (bb/read-cstring bb)))]

    {:type :NegotiateProtocolVersion
     :len len
     :minor-version minor-version
     :failed-params-count failed-params-count
     :failed-params failed-params}))


(defn parse-no-data [len bb]
  {:type :NoData
   :len len})


(defn parse-notice-response [len bb]
  (let [pid (bb/read-int32 bb)
        channel (bb/read-cstring bb)
        message (bb/read-cstring bb)]
    {:type :NotificationResponse
     :len len
     :pid pid
     :channel channel
     :message message}))


(defn parse-param-description [len bb]
  (let [param-count
        (bb/read-int16 bb)

        param-types
        (vec
         (for [i (range param-count)]
           (bb/read-int32 bb)))]
    {:type :ParameterDescription
     :len len
     :param-count param-count
     :param-types param-types}))


(defn parse-parse-complete [len bb]
  {:type :ParseComplete
   :len len})


(defn parse-portal-suspended [len bb]
  {:type :PortalSuspended
   :len len})


(defn parse-message-payload [lead len bb]

  (case (char lead)

    \T
    (parse-row-description len bb)

    \S
    (parse-param-status len bb)

    \R
    (parse-auth-response len bb)

    \K
    (parse-backend-data len bb)

    \Z
    (parse-ready-for-query len bb)

    \E
    (parse-error-response len bb)

    \N
    (parse-notice-response len bb)

    \I
    (parse-empty-query-response len bb)

    \V
    (parse-function-call-response len bb)

    \D
    (parse-data-row len bb)

    \A
    (parse-notice-response len bb)

    \s
    (parse-portal-suspended len bb)

    \1
    (parse-parse-complete len bb)

    \2
    (parse-bind-complete len bb)

    \3
    (parse-close-complete len bb)

    \C
    (parse-command-complete len bb)

    \v
    (parse-negotiate-protocol-version len bb)

    \t
    (parse-param-description len bb)

    \n
    (parse-no-data len bb)

    ;; else
    (throw (ex-info "Unhandled server message"
                    {:lead lead
                     :len len
                     :bb bb}))))


(defn read-message-payload [^SocketChannel chan bb-header]

  (bb/rewind bb-header)

  (let [lead (bb/read-byte bb-header)
        len (- (bb/read-int32 bb-header) 4)
        bb (bb/allocate len)
        read (.read chan bb)]

    (bb/rewind bb)

    (cond

      (= read len)
      (parse-message-payload lead len bb)

      :else
      (throw (ex-info "Inconsistent read"
                      {:read read
                       :lead lead
                       :len len
                       :bb-header bb-header
                       :bb :bb})))))


(defn read-message [^SocketChannel chan]
  (let [bb-header (bb/allocate 5)
        read (.read chan bb-header)]

    (cond

      (= read 5)
      (read-message-payload chan bb-header)

      (= read -1)
      nil

      :else
      (throw (ex-info "Inconsistent read"
                      {:read read
                       :bb-header bb-header})))))


(defn read-messages [^SocketChannel chan]
  (lazy-seq
   (when-let [message (read-message chan)]
     (cons message (read-messages chan)))))


(defn make-startup [database user]

  (let [len (+ 4 4 4 (count user) 1
               1 8 1 (count database) 1 1)]

    (doto (bb/allocate len)
      (bb/write-int32 len)
      (bb/write-int32 const/PROT_VER_14)
      (bb/write-cstring "user")
      (bb/write-cstring user)
      (bb/write-cstring "database")
      (bb/write-cstring database)
      (bb/write-byte 0))))


(defn byte-count
  ([string]
   (byte-count string "UTF-8"))

  ([^String string ^String encoding]
   (-> string (.getBytes encoding) (count))))


(defn make-query [query]
  (let [len (+ 4 (byte-count query) 1)]

    (doto (bb/allocate (inc len))
      (bb/write-byte \Q)
      (bb/write-int32 len)
      (bb/write-cstring query))))


(defn make-clear-text-password [password]

  (let [len
        (+ 4 (byte-count password) 1)]

    (doto (bb/allocate (inc len))
      (bb/write-byte \p)
      (bb/write-int32 len)
      (bb/write-cstring password))))


(defn make-md5-password
  [^String user ^String password ^bytes salt]

  ;; concat('md5', md5(concat(md5(concat(password, username)), random-salt)))

  (let [creds
        (-> (str password user)
            codec/str->bytes
            codec/md5
            codec/bytes->hex
            codec/str->bytes)

        hashed-pass
        (->> (codec/concat-bytes creds salt)
             codec/md5
             codec/bytes->hex
             (str "md5"))

        len
        (+ 4 (count hashed-pass) 1)]

    (doto (bb/allocate (inc len))
      (bb/write-byte \p)
      (bb/write-int32 len)
      (bb/write-cstring hashed-pass))))


(defn make-sasl-init-response
  [^String method ^String message]

  (let [buf
        (codec/str->bytes message )

        buf-len
        (alength buf)

        len
        (+ 4 (count method) 1 4 buf-len)]

    (doto (bb/allocate (inc len))
      (bb/write-byte \p)
      (bb/write-int32 len)
      (bb/write-cstring method)
      (bb/write-int32 buf-len)
      (bb/write-bytes buf))))


(defn make-sasl-response [^String client-message]

  (let [len
        (+ 4 (byte-count client-message))]

    (doto (bb/allocate (inc len))
      (bb/write-byte \p)
      (bb/write-int32 len)
      (bb/write-bytes (codec/str->bytes client-message)))))
