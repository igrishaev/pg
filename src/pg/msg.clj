(ns pg.msg
  (:import
   java.nio.ByteBuffer
   java.nio.channels.SocketChannel)
  (:require
   [pg.error :as e]
   [pg.bytes :as b]
   [pg.const :as const]
   [pg.codec :as codec]
   [pg.bb :as bb]))


(defn parse-row-description [bb]

  (let [field-count
        (bb/read-int16 bb)

        fields
        (loop [i 0
               acc! (transient [])]
          (if (= i field-count)
            (persistent! acc!)
            (recur
             (inc i)
             (conj! acc!
                    {:index       i
                     :name        (bb/read-cstring bb)
                     :table-id    (bb/read-int32 bb)
                     :column-id   (bb/read-int16 bb)
                     :type-id     (bb/read-int32 bb)
                     :type-len    (bb/read-int16 bb)
                     :type-mod    (bb/read-int32 bb)
                     :format      (bb/read-int16 bb)}))))]

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

  (let [param (-> bb
                  bb/read-cstring
                  codec/bytes->str)
        value (-> bb
                  bb/read-cstring
                  codec/bytes->str)]

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
         (let [item
               (-> bb
                   bb/read-cstring
                   codec/bytes->str)]
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
      (e/error! "Unknown authentication message"
                {:status status
                 :bb bb
                 :in ::here}))))


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
              (let [field-text
                    (bb/read-cstring bb)]
                (recur (conj acc {:label (char field-type)
                                  :bytes field-text}))))))]
    {:type :ErrorResponse
     :errors errors}))


(defn parse-notice-response [bb]
  (let [messages
        (loop [acc []]
          (let [field-type (bb/read-byte bb)]
            (if (zero? field-type)
              acc
              (let [field-bytes
                    (bb/read-cstring bb)]
                (recur (conj acc
                             {:type field-type
                              :message field-bytes}))))))]

    {:type :NoticeResponse
     :messages messages}))


(defn parse-bind-complete [bb]
  {:type :BindComplete})


(defn parse-close-complete [bb]
  {:type :CloseComplete})


(defn parse-command-complete [bb]
  {:type :CommandComplete
   :tag (bb/read-cstring bb)})


(defn parse-data-row [bb]
  (let [amount
        (bb/read-int16 bb)

        columns
        (loop [i 0
               acc! (transient [])]
          (if (= i amount)
            (persistent! acc!)
            (let [len (bb/read-int32 bb)
                  col (when-not (= len -1)
                        (bb/read-bytes bb len))]
              (recur (inc i)
                     (conj! acc! col)))))]

    {:type :DataRow
     :columns columns}))


(defn parse-empty-query-response [bb]
  {:type :EmptyQueryResponse})


(defn parse-function-call-response [bb]
  (let [res-len
        (bb/read-int32 bb)

        res-val
        (when-not (= res-len -1)
          (bb/read-bytes bb res-len))]

    {:type :FunctionCallResponse
     :len res-len
     :result res-val}))


(defn parse-negotiate-protocol-version [bb]
  (let [minor-version
        (bb/read-int32 bb)

        failed-params-count
        (bb/read-int32 bb)

        failed-params
        (bb/read-cstrings bb failed-params-count)]

    {:type :NegotiateProtocolVersion
     :minor-version minor-version
     :failed-params-count failed-params-count
     :failed-params failed-params}))


(defn parse-no-data [bb]
  {:type :NoData})


(defn parse-notification-response [bb]
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
        (bb/read-int32s bb param-count)]

    {:type :ParameterDescription
     :param-count param-count
     :param-types param-types}))


(defn parse-parse-complete [bb]
  {:type :ParseComplete})


(defn parse-portal-suspended [bb]
  {:type :PortalSuspended})


(defn parse-message-payload [lead bb]

  (case (char lead)

    \Z
    (parse-ready-for-query bb)

    \T
    (parse-row-description bb)

    \S
    (parse-param-status bb)

    \R
    (parse-auth-response bb)

    \K
    (parse-backend-data bb)

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
    (parse-notification-response bb)

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
    (e/error! "Unhandled server message"
              {:lead lead
               :bb bb
               :in ::here})))


(defn read-bb [^SocketChannel ch ^ByteBuffer bb]
  (while (not (zero? (bb/remaining bb)))
    (.read ch bb)))


(defn read-message-payload [^SocketChannel ch bb-header]

  (bb/rewind bb-header)

  (let [lead (bb/read-byte bb-header)
        len (- (bb/read-int32 bb-header) 4)
        bb (bb/allocate len)]

    (read-bb ch bb)
    (bb/rewind bb)

    (parse-message-payload lead bb)))


(defn read-message [^SocketChannel ch]
  (let [bb (bb/allocate 5)]
    (read-bb ch bb)
    (read-message-payload ch bb)))


(defn read-messages [^SocketChannel ch]
  (lazy-seq
   (when-let [message (read-message ch)]
     (cons message (read-messages ch)))))


(defn make-startup
  [^String database ^String user ^Integer protocol-version]

  (let [len (+ 4 4 4 (count user) 1
               1 8 1 (count database) 1 1)]

    (doto (bb/allocate len)
      (bb/write-int32 len)
      (bb/write-int32 protocol-version)
      (bb/write-cstring (codec/str->bytes "user"))
      (bb/write-cstring (codec/str->bytes user))
      (bb/write-cstring (codec/str->bytes "database"))
      (bb/write-cstring (codec/str->bytes database))
      (bb/write-byte 0))))


(defn make-sync []
  (doto (bb/allocate 5)
    (bb/write-byte \S)
    (bb/write-int32 4)))


(defn make-flush []
  (doto (bb/allocate 5)
    (bb/write-byte \H)
    (bb/write-int32 4)))


(defn make-query [^bytes query]
  (let [len (+ 4 (alength query) 1)]
    (doto (bb/allocate (inc len))
      (bb/write-byte \Q)
      (bb/write-int32 len)
      (bb/write-cstring query))))


(defn make-clear-text-password [^bytes password]
  (let [len
        (+ 4 (alength password) 1)]
    (doto (bb/allocate (inc len))
      (bb/write-byte \p)
      (bb/write-int32 len)
      (bb/write-cstring password))))


(defn make-md5-password
  [^bytes hashed-pass]
  (let [len
        (+ 4 (alength hashed-pass) 1)]
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
      (bb/write-cstring (codec/str->bytes method))
      (bb/write-int32 buf-len)
      (bb/write-bytes buf))))


(defn make-sasl-response [^String client-message]
  (let [len
        (+ 4 (count client-message))]
    (doto (bb/allocate (inc len))
      (bb/write-byte \p)
      (bb/write-int32 len)
      (bb/write-bytes (codec/str->bytes client-message)))))


(defn make-parse
  [^bytes stmt-name ^bytes query oid-types]

  (let [type-count
        (count oid-types)

        len
        (+ 4
           (alength stmt-name) 1
           (alength query) 1
           2
           (* (count oid-types) 4))

        bb
        (bb/allocate (inc len))]

    (doto bb
      (bb/write-byte \P)
      (bb/write-int32 len)
      (bb/write-cstring stmt-name)
      (bb/write-cstring query)
      (bb/write-int16 type-count)
      (bb/write-int32s oid-types))))


(defn make-execute
  ([portal]
   (make-execute portal 0))

  ([portal amount]
   (let [len
         (+ 4 (count portal) 1 4)]
     (doto (bb/allocate (inc len))
       (bb/write-byte \E)
       (bb/write-int32 len)
       (bb/write-cstring portal)
       (bb/write-int32 amount)))))


(defn make-describe-statement
  [^bytes statement]
  (let [len
        (+ 4 1 (alength statement) 1)]
    (doto (bb/allocate (inc len))
      (bb/write-byte \D)
      (bb/write-int32 len)
      (bb/write-byte \S)
      (bb/write-cstring statement))))


(defn make-describe-portal [^bytes portal]
  (let [len
        (+ 4 1 (alength portal) 1)]
    (doto (bb/allocate (inc len))
      (bb/write-byte \D)
      (bb/write-int32 len)
      (bb/write-byte \P)
      (bb/write-cstring portal))))


(defn make-bind [^bytes portal-name
                 ^bytes prstmt-name
                 param-formats
                 param-values
                 column-formats]

  (let [len
        (+ 4
           (alength portal-name) 1
           (alength prstmt-name) 1

           2
           (* 2 (count param-formats))

           2
           (reduce
            (fn -reduce [result ^bytes bytes]
              (+ result 4 (if (some? bytes)
                            (alength bytes)
                            0)))
            0
            param-values)

           2
           (* 2 (count column-formats)))

        bb
        (bb/allocate (inc len))]

    (doto bb
      (bb/write-byte \B)
      (bb/write-int32 len)

      (bb/write-cstring portal-name)
      (bb/write-cstring prstmt-name)
      (bb/write-int16 (count param-formats))
      (bb/write-int16s param-formats)
      (bb/write-int16 (count param-values)))

    (doseq [^bytes bytes param-values]
      (if (nil? bytes)
        (bb/write-int32 bb -1)
        (do
          (bb/write-int32 bb (alength bytes))
          (bb/write-bytes bb bytes))))

    (doto bb
      (bb/write-int16 (count column-formats))
      (bb/write-int16s column-formats))))


(defn make-cancell-request [pid secret-key]
  (doto (bb/allocate 16)
    (bb/write-int32 16)
    (bb/write-int32 const/CANCELL_CODE)
    (bb/write-int32 pid)
    (bb/write-int32 secret-key)))


(defn make-close-statement [^bytes statement]
  (let [len
        (+ 4 1 (alength statement) 1)]
    (doto (bb/allocate (inc len))
      (bb/write-byte \C)
      (bb/write-int32 len)
      (bb/write-byte \S)
      (bb/write-cstring statement))))


(defn make-close-portal [^bytes portal]
  (let [len
        (+ 4 1 (alength portal) 1)]
    (doto (bb/allocate (inc len))
      (bb/write-byte \C)
      (bb/write-int32 len)
      (bb/write-byte \P)
      (bb/write-cstring portal))))


(defn make-ssl-request []
  (doto (bb/allocate 8)
    (bb/write-int32 8)
    (bb/write-int32 const/SSL_REQUEST)))


(defn make-terminate []
  (doto (bb/allocate 5)
    (bb/write-byte \X)
    (bb/write-int32 4)))


(defn make-function-call
  [proc-oid args-format args-bytes result-format]

  (let [len
        (+ 4
           4
           2
           (* 2 (count args-bytes))
           2
           (reduce
            (fn [res ^bytes bytes]
              (+ res 4 (if (some? bytes)
                         (alength bytes)
                         0)))
            0
            args-bytes)
           2)

        bb
        (bb/allocate (inc len))]

    (doto bb
      (bb/write-byte \F)
      (bb/write-int32 len)
      (bb/write-int32 proc-oid)
      (bb/write-int16 (count args-format))
      (bb/write-int16s args-format)
      (bb/write-int16 (count args-bytes)))

    (doseq [^bytes bytes args-bytes]
      (if (nil? bytes)
        (bb/write-int32 bb -1)
        (do
          (bb/write-int32 bb (alength bytes))
          (bb/write-bytes bb bytes))))

    (doto bb
      (bb/write-int16 result-format))))
