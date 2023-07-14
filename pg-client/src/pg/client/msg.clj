(ns pg.client.msg
  (:import
   clojure.lang.Keyword
   java.util.Map
   java.util.List
   java.io.ByteArrayOutputStream)
  (:require
   [pg.client.out :as out]
   [pg.client.bb :as bb]
   [pg.client.coll :as coll]
   [pg.encode.txt :as txt]))


;; Auth SASL

;; FunctionCall
;; FunctionCallResponse

;; SSLRequest

;; CopyData
;; CopyDone
;; CopyFail
;; CopyInResponse
;; CopyOutResponse
;; CopyBothResponse



(defmacro get-server-encoding [opt]
  `(or (get ~opt :server-encoding) "UTF-8"))


(defmacro get-client-encoding [opt]
  `(or (get ~opt :client-encoding) "UTF-8"))


(defn parse-AuthenticationOk [bb opt]
  {:msg :AuthenticationOk
   :status 0})


(defn parse-AuthenticationMD5Password [bb opt]

  (let [salt
        (bb/read-bytes bb 4)]

    {:msg :AuthenticationMD5Password
     :status 5
     :salt salt}))


(defn parse-AuthenticationCleartextPassword [bb opt]
  {:msg :AuthenticationCleartextPassword
   :status 3})


(defn parse-AuthenticationResponse [bb opt]

  (let [status (bb/read-int32 bb)]

    (case status

      0
      (parse-AuthenticationOk bb opt)

      3
      (parse-AuthenticationCleartextPassword bb opt)

      5
      (parse-AuthenticationMD5Password bb opt)

      ;; else

      (throw (ex-info "Cannot parse authentication response"
                      {:bb (bb/to-vector bb)
                       :opt opt
                       :status status})))))


(defn parse-ReadyForQuery [bb opt]

  (let [tx-status
        (-> bb bb/read-byte char str keyword)]

    {:msg :ReadyForQuery
     :tx-status tx-status}))


(defn parse-CommandComplete [bb opt]

  (let [encoding
        (get-server-encoding opt)

        tag
        (bb/read-cstring bb encoding)]

    {:msg :CommandComplete
     :tag tag}))


(defn parse-CloseComplete [bb opt]
  {:msg :CloseComplete})


(defn parse-ParseComplete [bb opt]
  {:msg :ParseComplete})


(defn parse-DataRow [bb opt]

  (let [value-count
        (bb/read-int16 bb)

        values
        (coll/for-n [i value-count]
          (let [len (bb/read-int32 bb)]
            (when-not (= len -1)
              (bb/read-bytes bb len))))]

    {:msg :DataRow
     :value-count value-count
     :values values}))


(defn parse-RowDescription [bb opt]

  (let [encoding
        (get-server-encoding opt)

        column-count
        (bb/read-int16 bb)

        columns
        (coll/for-n [i column-count]
          {:index      i
           :name       (bb/read-cstring bb encoding)
           :table-oid  (bb/read-int32 bb)
           :column-oid (bb/read-int16 bb)
           :type-oid   (bb/read-int32 bb)
           :type-len   (bb/read-int16 bb)
           :type-mod   (bb/read-int32 bb)
           :format     (bb/read-int16 bb)})]

    {:msg :RowDescription
     :column-count column-count
     :columns columns}))


(defn parse-token ^Keyword [^Byte b]
  (case (char b)
    \S :severity
    \V :verbosity
    \C :code
    \M :message
    \D :detail
    \H :hint
    \P :position
    \p :position-internal
    \q :query
    \W :stacktrace
    \s :schema
    \t :table
    \c :column
    \d :datatype
    \n :constraint
    \F :file
    \L :line
    \R :function
    (-> b char str keyword)))


(defn parse-ParameterStatus [bb opt]

  (let [encoding
        (get-server-encoding opt)

        param
        (bb/read-cstring bb encoding)

        value
        (bb/read-cstring bb encoding)]

    {:msg :ParameterStatus
     :param param
     :value value}))


(defn parse-BackendKeyData [bb opt]

  (let [pid
        (bb/read-int32 bb)

        secret-key
        (bb/read-int32 bb)]

    {:msg :BackendKeyData
     :pid pid
     :secret-key secret-key}))


(defn parse-ErrorResponse [bb opt]

  (let [encoding
        (get-server-encoding opt)

        errors
        (loop [acc {}]
          (let [b (bb/read-byte bb)]
            (if (zero? b)
              acc
              (let [token
                    (parse-token b)
                    field
                    (bb/read-cstring bb encoding)]
                (recur (assoc acc token field))))))]

    {:msg :ErrorResponse
     :errors errors}))


(defn parse-BindComplete [bb opt]
  {:msg :BindComplete})


(defn parse-PortalSuspended [bb opt]
  {:msg :PortalSuspended})


(defn parse-EmptyQueryResponse [bb opt]
  {:msg :EmptyQueryResponse})


(defn parse-NoticeResponse [bb opt]

  (let [encoding
        (get-server-encoding opt)

        fields
        (loop [acc {}]
          (let [b (bb/read-byte bb)]
            (if (zero? b)
              acc
              (let [token
                    (parse-token b)
                    field
                    (bb/read-cstring bb encoding)]
                (recur (assoc acc token field))))))]

    {:msg :NoticeResponse
     :fields fields}))


(defn parse-ParameterDescription [bb opt]

  (let [param-count
        (bb/read-int16 bb)

        param-oids
        (coll/for-n [_ param-count]
          (bb/read-int32 bb))]

    {:msg :ParameterDescription
     :param-count param-count
     :param-oids param-oids}))


(defn parse-NoData [bb opt]
  {:msg :NoData})


(defn parse-NotificationResponse [bb opt]

  (let [encoding
        (get-server-encoding opt)

        pid
        (bb/read-int32 bb)

        channel
        (bb/read-cstring bb encoding)

        message
        (bb/read-cstring bb encoding)]

    {:msg :NotificationResponse
     :pid pid
     :channel channel
     :message message}))


(defn parse-NegotiateProtocolVersion [bb opt]

  (let [encoding
        (get-server-encoding opt)

        version
        (bb/read-int32 bb)

        param-count
        (bb/read-int32 bb)

        params
        (coll/for-n [_ param-count]
          (bb/read-cstring bb encoding))]

    {:msg :NegotiateProtocolVersion
     :version version
     :param-count param-count
     :params params}))


(defn parse-message

  [tag bb opt]

  (case tag

    \A
    (parse-NotificationResponse bb opt)

    \n
    (parse-NoData bb opt)

    \v
    (parse-NegotiateProtocolVersion bb opt)

    \N
    (parse-NoticeResponse bb opt)

    \I
    (parse-EmptyQueryResponse bb opt)

    \s
    (parse-PortalSuspended bb opt)

    \1
    (parse-ParseComplete bb opt)

    \2
    (parse-BindComplete bb opt)

    \3
    (parse-CloseComplete bb opt)

    \S
    (parse-ParameterStatus bb opt)

    \C
    (parse-CommandComplete bb opt)

    \D
    (parse-DataRow bb opt)

    \T
    (parse-RowDescription bb opt)

    \R
    (parse-AuthenticationResponse bb opt)

    \Z
    (parse-ReadyForQuery bb opt)

    \E
    (parse-ErrorResponse bb opt)

    \K
    (parse-BackendKeyData bb opt)

    \t
    (parse-ParameterDescription bb opt)

    ;; else

    (throw (ex-info "Cannot parse a message"
                    {:tag tag
                     :opt opt
                     :bb (bb/to-vector bb)}))))


(defn to-bb

  ([^ByteArrayOutputStream out]
   (to-bb nil out))

  ([^Character c ^ByteArrayOutputStream out]

   (let [buf
         (.toByteArray out)

         buf-len
         (alength buf)

         bb-len
         (+ (if (nil? c) 0 1)
            4
            buf-len)

         bb
         (bb/allocate bb-len)]

     (when-not (nil? c)
       (bb/write-byte bb c))

     (doto bb
       (bb/write-int32 (+ 4 buf-len))
       (bb/write-bytes buf)))))


(defn make-PasswordMessage [password]
  {:msg :PasswordMessage
   :password password})


(defn encode-PasswordMessage
  [{:keys [password]}
   opt]

  (let [encoding
        (get-client-encoding opt)

        out
        (doto (out/create)
          (out/write-cstring password encoding))]

    (to-bb \p out)))


(defn make-Terminate []
  {:msg :Terminate})


(defn encode-Terminate [_ _]
  (doto (bb/allocate 5)
    (bb/write-byte \X)
    (bb/write-int32 4)))


(defn make-CancelRequest [code pid secret-key]
  {:msg :CancelRequest
   :code code
   :pid pid
   :secret-key secret-key})


(defn encode-CancelRequest
  [{:keys [code
           pid
           secret-key]}
   opt]

  (doto (bb/allocate 16)
    (bb/write-int32 16)
    (bb/write-int32 code)
    (bb/write-int32 pid)
    (bb/write-int32 secret-key)))


(defn make-SSLRequest [ssl-code]
  {:msg :SSLRequest
   :ssl-code ssl-code})


(defn encode-SSLRequest [{:keys [ssl-code]} _]
  (doto (bb/allocate 8)
    (bb/write-int32 8)
    (bb/write-int32 ssl-code)))


(defn make-StartupMessage
  [^Integer protocol-version
   ^String user
   ^String database
   ^Map options]

  {:msg              :StartupMessage
   :protocol-version protocol-version
   :user             user
   :database         database
   :options          options})


(defn encode-StartupMessage
  [{:keys [^Integer protocol-version
           ^String user
           ^String database
           ^Map options]}
   opt]

  (let [^String encoding
        (get-client-encoding opt)

        out
        (doto (out/create)
          (out/write-int32 protocol-version)
          (out/write-cstring "user" encoding)
          (out/write-cstring user encoding)
          (out/write-cstring "database" encoding)
          (out/write-cstring database encoding))]

    (coll/do-map [[k v] options]
      (doto out
        (out/write-cstring k encoding)
        (out/write-cstring v encoding)))

    (doto out
      (out/write-byte 0))

    (to-bb out)))


(defn make-Close [source-type source]
  {:msg :Close
   :source-type source-type
   :source source})


(defn encode-Close
  [{:keys [^Character source-type
           ^String source]}
   opt]

  (let [encoding
        (get-client-encoding opt)

        out
        (doto (out/create)
          (out/write-char source-type)
          (out/write-cstring source encoding))]

    (to-bb \C out)))


(defn make-Parse [^String statement
                  ^String query
                  ^List param-oids]

  {:msg :Parse
   :statement statement
   :query query
   :param-oids param-oids})


(defn encode-Parse
  [{:keys [^String statement
           ^String query
           ^List param-oids]}
   opt]

  (let [encoding
        (get-client-encoding opt)

        out
        (doto (out/create)
          (out/write-cstring statement encoding)
          (out/write-cstring query encoding)
          (out/write-int16 (count param-oids)))]

    (coll/do-list [_ oid param-oids]
      (out/write-int32 out oid))

    (to-bb \P out)))


(defn make-Sync []
  {:msg :Sync})


(defn encode-Sync [_ _]
  (doto (bb/allocate 5)
    (bb/write-byte \S)
    (bb/write-int32 4)))


(defn make-Flush []
  {:msg :Flush})


(defn encode-Flush [_ _]
  (doto (bb/allocate 5)
    (bb/write-byte \H)
    (bb/write-int32 4)))


(defn make-Describe
  [^Character source-type
   ^String source]

  {:msg :Describe
   :source-type source-type
   :source source})


(defn encode-Describe
  [{:keys [^Character source-type
           ^String source]}
   opt]

  (let [encoding
        (get-client-encoding opt)

        out
        (doto (out/create)
          (out/write-char source-type)
          (out/write-cstring source encoding))]

    (to-bb \D out)))


(defn make-Query [query]
  {:msg :Query
   :query query})


(defn encode-Query
  [{:keys [^String query]} opt]

  (let [encoding
        (get-client-encoding opt)

        out
        (doto (out/create)
          (out/write-cstring query encoding))]

    (to-bb \Q out)))


(defn make-Bind [^String portal
                 ^String statement
                 ^List param-formats
                 ^List params
                 ^List param-oids
                 ^List column-formats]

  {:msg :Bind
   :portal         portal
   :statement      statement
   :param-formats  param-formats
   :params         params
   :param-oids     param-oids
   :column-formats column-formats})


(defn encode-Bind
  [{:as message
    :keys [^String portal
           ^String statement
           ^List param-formats
           ^List params
           ^List param-oids
           ^List column-formats]}
   opt]

  (let [len-params (count params)
        len-param-oids (count param-oids)]
    (when-not (= len-params len-param-oids)
      (let [msg
            (format "Wrong parameters count: %s (must be %s)"
                    len-params len-param-oids)]
        (throw (ex-info msg message)))))

  (let [^String encoding
        (get-client-encoding opt)

        out
        (doto (out/create)
          (out/write-cstring portal encoding)
          (out/write-cstring statement encoding)
          (out/write-int16 (count param-formats))
          (out/write-int16s param-formats))]

    (out/write-int16 out (count params))
    (coll/do-n [i (count params)]

      (let [param (.get params i)
            param-oid (.get param-oids i)]

        (if (nil? param)

          (out/write-int32 out -1)

          (let [^String encoded
                (txt/encode param param-oid opt)

                buf
                (.getBytes encoded encoding)

                len
                (alength buf)]

            (out/write-int32 out len)
            (out/write-bytes out buf)))))

    (out/write-int16 out (.size column-formats))
    (out/write-int16s out column-formats)

    (to-bb \B out)))


(defn make-Execute [^String portal
                    ^Integer row-count]

  {:msg :Execute
   :portal portal
   :row-count row-count})


(defn encode-Execute
  [{:keys [portal
           row-count]}
   opt]

  (let [encoding
        (get-client-encoding opt)

        out
        (doto (out/create)
          (out/write-cstring portal encoding)
          (out/write-int32 row-count))]

    (to-bb \E out)))


(defn encode-message [{:as message :keys [msg]} opt]

  (case msg

    :Terminate
    (encode-Terminate message opt)

    :Query
    (encode-Query message opt)

    :Close
    (encode-Close message opt)

    :StartupMessage
    (encode-StartupMessage message opt)

    :PasswordMessage
    (encode-PasswordMessage message opt)

    :Parse
    (encode-Parse message opt)

    :Sync
    (encode-Sync message opt)

    :CancelRequest
    (encode-CancelRequest message opt)

    :Flush
    (encode-Flush message opt)

    :Describe
    (encode-Describe message opt)

    :Bind
    (encode-Bind message opt)

    :Execute
    (encode-Execute message opt)

    :SSLRequest
    (encode-SSLRequest message opt)

    ;; else

    (throw (ex-info "Cannot encode a message"
                    {:opt opt
                     :message message}))))
