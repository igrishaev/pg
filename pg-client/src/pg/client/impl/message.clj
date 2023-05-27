(ns pg.client.impl.message
  (:import
   java.util.Set
   java.util.Map
   java.util.List
   java.nio.ByteBuffer
   clojure.lang.Keyword)
  (:require
   [pg.bytes.array :as array]
   [pg.client.prot.connection :as connection]
   [pg.client.prot.message :as message]
   [pg.client.prot.result :as result]
   [pg.client.bytes :as bytes]
   [pg.client.codec :as codec]
   [pg.client.bb :as bb]))


(defrecord NoticeEntry
    [^Character tag
     ^String notice])


(defrecord NoticeResponse
    [^List entries]

  message/IMessage

  (handle [this result connection]
    (connection/handle-NoticeResponse connection this)
    result)

  (from-bb [this bb connection]

    (let [encoding
          (connection/get-server-encoding connection)

          entries
          (loop [acc []]
            (let [b (bb/read-byte bb)]
              (if (zero? b)
                acc
                (let [notice
                      (bb/read-cstring bb encoding)
                      entry
                      (new NoticeEntry (char b) notice)]
                  (recur (conj acc entry))))))]

      (assoc this :entries entries))))


(defmethod message/tag->message \N [_]
  (new NoticeResponse nil))


(defn bb-encode ^ByteBuffer [^String encoding tag parts]

  (let [len-payload
        (reduce
         (fn [result part]
           (cond

             (bytes/byte? part)
             (inc result)

             (bytes? part)
             (+ result (alength ^bytes part))

             (string? part)
             (+ result (alength (.getBytes ^String part encoding)) 1)

             :else
             (throw (ex-info "Wrong part type" {:tag tag :part part}))))
         0
         parts)

        bb
        (bb/allocate (+ (if tag 1 0) 4 len-payload))]

    (when tag
      (bb/write-byte bb tag))

    (bb/write-int32 bb (+ 4 len-payload))

    (doseq [part parts]
      (cond

        (bytes/byte? part)
        (bb/write-byte bb part)

        (bytes? part)
        (bb/write-bytes bb part)

        (string? part)
        (bb/write-cstring bb part encoding)))

    (bb/rewind bb)
    bb))


(defrecord Sync []

  message/IMessage

  (to-bb [this connection]
    (let [encoding
          (connection/get-client-encoding connection)]
      (bb-encode encoding \S nil))))


(defrecord StartupMessage
    [^Integer protocol-version
     ^String user
     ^String database
     ^Map options]

  message/IMessage

  (to-bb [this connection]
    (let [encoding
          (connection/get-client-encoding connection)]
      (bb-encode encoding
                 nil
                 [(array/arr32 protocol-version)
                  "user"
                  user
                  "database"
                  database
                  (byte 0)]))))


(defrecord AuthenticationOk
    [^Integer status]

  message/IMessage

  (handle [this result connection]
    result))


(defrecord AuthenticationKerberosV5
    [^Integer status])


(defrecord AuthenticationSCMCredential
    [^Integer status])


(defrecord AuthenticationGSS
    [^Integer status])


(defrecord AuthenticationGSSContinue
    [^Integer status
     ^bytes auth])


(defrecord AuthenticationSSPI
    [^Integer status])


(defrecord AuthenticationSASL
    [^Integer status
     ^Set sasl-types])


(defrecord AuthenticationSASLContinue
    [^Integer status
     ^String server-first-message])


(defrecord AuthenticationSASLFinal
    [^Integer status
     ^String server-final-message])


(defmethod message/status->message 0
  [status bb connection]
  (new AuthenticationOk status))


(defmethod message/status->message 2
  [status bb connection]
  (new AuthenticationKerberosV5 status))


(defmethod message/status->message 6
  [status bb connection]
  (new AuthenticationSCMCredential status))


(defmethod message/status->message 7
  [status bb connection]
  (new AuthenticationGSS status))


(defrecord AuthenticationResponse
    [^Integer status]

  message/IMessage

  (from-bb [this bb connection]

    (let [status (bb/read-int32 bb)]
      (message/status->message status bb connection))))


(defmethod message/tag->message \R [_]
  (new AuthenticationResponse nil))


(defrecord ReadyForQuery
    [^Character tx-status]

  message/IMessage

  (handle [this result connection]
    (connection/set-tx-status connection tx-status)
    result)

  (from-bb [this bb connection]
    (let [tx-status (char (bb/read-byte bb))]
      (assoc this :tx-status tx-status))))

(defmethod message/tag->message \Z [_]
  (new ReadyForQuery nil))


(defrecord BackendKeyData
    [^Integer pid
     ^Integer secret-key]

  message/IMessage

  (handle [this result connection]
    (connection/set-pid connection pid)
    (connection/set-secret-key connection secret-key)
    result)

  (from-bb [this bb connection]

    (let [pid
          (bb/read-int32 bb)

          secret-key
          (bb/read-int32 bb)]

      (assoc this
             :pid pid
             :secret-key secret-key))))


(defmethod message/tag->message \K [_]
  (new BackendKeyData nil nil))


(defrecord RowColumn
    [^Integer index
     ^String  name
     ^Integer table-oid
     ^Short   column-oid
     ^Integer type-oid
     ^Short   type-len
     ^Integer type-mod
     ^Short   format])


(defrecord RowDescription
    [^Integer column-count
     ^List columns]

  message/IMessage

  (handle [this result connection]
    (doto result
      (result/add-RowDescription this)))

  (from-bb [this bb connection]

    (let [encoding
          (connection/get-server-encoding connection)

          column-count
          (bb/read-int16 bb)

          columns
          (loop [i 0
                 acc! (transient [])]
            (if (= i column-count)
              (persistent! acc!)
              (recur
               (inc i)
               (conj! acc!
                      (new RowColumn
                           i
                           (bb/read-cstring bb encoding)
                           (bb/read-int32 bb)
                           (bb/read-int16 bb)
                           (bb/read-int32 bb)
                           (bb/read-int16 bb)
                           (bb/read-int32 bb)
                           (bb/read-int16 bb))))))]

      (assoc this
             :column-count column-count
             :columns columns))))


(defmethod message/tag->message \T [_]
  (new RowDescription nil nil))


(defrecord ParameterStatus
    [^String param
     ^Object value]

  message/IMessage

  (handle [this result connection]
    (connection/set-parameter connection param value)
    result)

  (from-bb [this bb connection]

    (let [encoding
          (connection/get-server-encoding connection)

          param
          (-> bb (bb/read-cstring encoding))

          value
          (-> bb (bb/read-cstring encoding))]

      (assoc this
             :param param
             :value value))))


(defmethod message/tag->message \S [_]
  (new ParameterStatus nil nil))


(defrecord ErrorNode
    [^Character tag
     ^String message])


(defrecord ErrorResponse
    [^List errors]

  message/IMessage

  (handle [this result connection]
    (doto result
      (result/add-ErrorResponse this)))

  (from-bb [this bb connection]

    (let [encoding
          (connection/get-server-encoding connection)

          errors
          (loop [acc []]
            (let [field-tag (bb/read-byte bb)]
              (if (zero? field-tag)
                acc
                (let [field-text
                      (bb/read-cstring bb encoding)

                      error
                      (new ErrorNode
                           (char field-tag)
                           field-text)]

                  (recur (conj acc error))))))]

      (assoc this :errors errors))))


(defmethod message/tag->message \E [_]
  (new ErrorResponse nil))


(defrecord EmptyQueryResponse
    []

  message/IMessage

  (from-bb [this bb connection]
    this)

  (handle [this result connection]
    result))


(defmethod message/tag->message \I [_]
  (new EmptyQueryResponse))


(defrecord DataRow
    [^List values]

  message/IMessage

  (handle [this result connection]
    (doto result
      (result/add-DataRow this)))

  (from-bb [this bb connection]

    (let [value-count
          (bb/read-int16 bb)

          values
          (loop [i 0
                 acc! (transient [])]
            (if (= i value-count)
              (persistent! acc!)
              (let [len (bb/read-int32 bb)
                    col (when-not (= len -1)
                          (bb/read-bytes bb len))]
                (recur (inc i)
                       (conj! acc! col)))))]

      (assoc this :values values))))


(defmethod message/tag->message \D [_]
  (new DataRow nil))


(defrecord NoData [])


(defrecord CommandComplete
    [^String tag]

  message/IMessage

  (handle [this result connection]
    (doto result
      (result/add-CommandComplete this)))

  (from-bb [this bb connection]

    (let [encoding
          (connection/get-server-encoding connection)

          tag
          (bb/read-cstring bb encoding)]

      (assoc this :tag tag))))


(defmethod message/tag->message \C [_]
  (new CommandComplete nil))


(defrecord PasswordMessage
    [^String password]

  message/IMessage

  (to-bb [this connection]
    (let [encoding
          (connection/get-client-encoding connection)]
      (bb-encode encoding \p [password]))))


(defrecord Query
    [^String query]

  message/IMessage

  (to-bb [this connection]
    (let [encoding
          (connection/get-client-encoding connection)]
      (bb-encode encoding \Q [query]))))


(defrecord Terminate []

  message/IMessage

  (to-bb [this connection]
    (bb-encode nil \X nil)))
