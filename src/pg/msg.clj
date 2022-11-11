(ns pg.msg
  (:import
   java.nio.channels.SocketChannel)
  (:require
   [pg.codec :as codec]
   [pg.bb :as bb]))

;; TODO: vec to loop


(defn parse-row-description [bb]

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
     :field-count field-count
     :fields fields}))


(defn coerce-param-value [value]
  (case value
    "off" false
    "on"  true
    ""    nil
    value))


(defn parse-param-status [bb]

  (let [param (bb/read-cstring bb)
        value (bb/read-cstring bb)]

    {:type :ParameterStatus
     :param param
     :value (coerce-param-value value)}))


(defn parse-auth-response [bb]

  (let [status (int (bb/read-int32 bb))]

    (case status

      0
      {:type :AuthenticationOk
       :status status}

      2
      {:type :AuthenticationKerberosV5
       :status status}

      3
      {:type :AuthenticationCleartextPassword
       :status status}

      5
      {:type :AuthenticationMD5Password
       :status status
       :salt (bb/read-bytes bb 4)}

      6
      {:type :AuthenticationSCMCredential
       :status status}

      7
      {:type :AuthenticationGSS
       :status status}

      8
      {:type :AuthenticationGSSContinue
       :status status
       :auth (bb/read-rest bb)}

      9
      {:type :AuthenticationSSPI
       :status status}

      10
      {:type :AuthenticationSASL
       :status status
       :sasl-types
       (loop [acc #{}]
         (let [item (bb/read-cstring bb)]
           (if (= item "")
             acc
             (recur (conj acc item)))))}

      11
      {:type :AuthenticationSASLContinue
       :status status
       :server-first-message
       (-> bb
           bb/read-rest
           codec/bytes->str)}

      12
      {:type :AuthenticationSASLFinal
       :status status
       :server-final-message
       (-> bb
           bb/read-rest
           codec/bytes->str)}

      ;; else
      (throw (ex-info "Unknown auth message"
                      {:status status
                       :bb bb})))))


(defn parse-backend-data [bb]
  (let [pid (bb/read-int32 bb)
        secret-key (bb/read-int32 bb)]
    {:type :BackendKeyData
     :pid pid
     :secret-key secret-key}))


(defn parse-ready-for-query [bb]
  (let [tx-status (char (bb/read-byte bb))]
    {:type :ReadyForQuery
     :tx-status tx-status}))


(defn parse-error-response [bb]
  (let [errors
        (loop [acc []]
          (let [field-type (bb/read-byte bb)]
            (if (zero? field-type)
              acc
              (let [field-text (bb/read-cstring bb)]
                (recur (conj acc {:type (char field-type)
                                  :text field-text}))))))]
    {:type :ErrorResponse
     :errors errors}))


(defn parse-notice-response [bb]
  (let [messages
        (loop [acc []]
          (let [field-type (bb/read-byte bb)]
            (if (zero? field-type)
              acc
              (let [field-text (bb/read-cstring bb)]
                (recur (conj acc {:type field-type :text field-text}))))))]
    {:type :NoticeResponse
     :messages messages}))


(defn parse-bind-complete [bb]
  {:type :BindComplete})


(defn parse-close-complete [bb]
  {:type :CloseComplete})


(defn parse-command-complete [bb]
  (let [tag (bb/read-cstring bb)]
    {:type :CommandComplete
     :tag tag}))


(defn parse-data-row [bb]
  (let [amount
        (bb/read-int16 bb)

        columns
        (loop [i 0
               result (transient [])]
          (if (= i amount)
            (persistent! result)
            (let [len (bb/read-int32 bb)
                  col (when-not (= len -1)
                        (bb/read-bytes bb len))]
              (recur (inc i) (conj! result col)))))]

    {:type :DataRow
     :columns columns}))


(defn parse-empty-query-response [bb]
  {:type :EmptyQueryResponse})


(defn parse-function-call-response [bb]
  (let [res-len (bb/read-int32 bb)
        res-val (bb/read-bytes bb res-len)]
    {:type :FunctionCallResponse
     :result res-val}))


(defn parse-negotiate-protocol-version [bb]
  (let [minor-version
        (bb/read-int32 bb)

        failed-params-count
        (bb/read-int32 bb)

        failed-params
        (vec
         (for [i (range failed-params-count)]
           (bb/read-cstring bb)))]

    {:type :NegotiateProtocolVersion
     :minor-version minor-version
     :failed-params-count failed-params-count
     :failed-params failed-params}))


(defn parse-no-data [bb]
  {:type :NoData})


(defn parse-notice-response [bb]
  (let [pid (bb/read-int32 bb)
        channel (bb/read-cstring bb)
        message (bb/read-cstring bb)]
    {:type :NotificationResponse
     :pid pid
     :channel channel
     :message message}))


(defn parse-param-description [bb]
  (let [param-count
        (bb/read-int16 bb)

        param-types
        (vec
         (for [i (range param-count)]
           (bb/read-int32 bb)))]
    {:type :ParameterDescription
     :param-count param-count
     :param-types param-types}))


(defn parse-parse-complete [bb]
  {:type :ParseComplete})


(defn parse-portal-suspended [bb]
  {:type :PortalSuspended})


(defn parse-message-payload [lead bb]

  (case (char lead)

    \T
    (parse-row-description bb)

    \S
    (parse-param-status bb)

    \R
    (parse-auth-response bb)

    \K
    (parse-backend-data bb)

    \Z
    (parse-ready-for-query bb)

    \E
    (parse-error-response bb)

    \N
    (parse-notice-response bb)

    \I
    (parse-empty-query-response bb)

    \V
    (parse-function-call-response bb)

    \D
    (parse-data-row bb)

    \A
    (parse-notice-response bb)

    \s
    (parse-portal-suspended bb)

    \1
    (parse-parse-complete bb)

    \2
    (parse-bind-complete bb)

    \3
    (parse-close-complete bb)

    \C
    (parse-command-complete bb)

    \v
    (parse-negotiate-protocol-version bb)

    \t
    (parse-param-description bb)

    \n
    (parse-no-data bb)

    ;; else
    (throw (ex-info "Unhandled server message"
                    {:lead lead
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
      (parse-message-payload lead bb)

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


(defn make-startup [database user protocol-version]

  (let [len (+ 4 4 4 (count user) 1
               1 8 1 (count database) 1 1)]

    (doto (bb/allocate len)
      (bb/write-int32 len)
      (bb/write-int32 protocol-version)
      (bb/write-cstring "user")
      (bb/write-cstring user)
      (bb/write-cstring "database")
      (bb/write-cstring database)
      (bb/write-byte 0))))


(defn make-sync []
  (doto (bb/allocate 5)
    (bb/write-byte \S)
    (bb/write-int32 4)))


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
