(ns pg.msg
  (:import
   java.nio.channels.SocketChannel
   java.io.InputStream)
  (:require
   [pg.bb :as bb]))


(defn parse-row-description [len bb]

  (let [field-count
        (bb/read-int16 bb)

        fields
        (doall
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


(defn parse-param-status [len bb]

  (let [param (bb/read-cstring bb)
        value (bb/read-cstring bb)]

    {:type :ParameterStatus
     :len len
     :param param
     :value value}))


#_
(defn parse-auth-ok [len bb]
  (let [status (bb/read-int32 bb)]
    {:type :AuthenticationOk
     :len len
     :status status}))


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
      (let [auth-list
            (loop [acc []]
              (let [item (bb/read-cstring bb)]
                (if (= item "")
                  acc
                  (recur (conj acc item)))))]
        {:type :AuthenticationSASL
         :len len
         :status status
         :auth-list auth-list})

      11
      (let [auth (bb/read-rest bb)]
        {:type :AuthenticationSASLContinue
         :len len
         :status status
         :auth auth})

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
  (let [tx-status (bb/read-byte)]
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
                (recur (conj acc {:type field-type :text field-text}))))))]
    {:type :ErrorResponse
     :len len
     :errors errors}))


(defn read-message [^SocketChannel chan]
  (let [bb-header (bb/allocate 5)
        read (.read chan bb-header)]

    (when-not (zero? read)

      (let [lead (bb/read-byte bb-header)
            len (- (bb/read-int32 bb-header) 4)
            bb (bb/allocate len)]

        (bb/rewind bb)

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

          ;; else
          (throw (ex-info "Unhandled server message"
                          {:lead lead
                           :len len
                           :bb-header bb-header
                           :bb bb})))))))


(defn make-startup [database user]

  (let [len (+ 4 4 4 (count user) 1
               1 8 1 (count database) 1 1)
        bb (bb/allocate len)]

    (doto bb
      (bb/write-int32 len)
      (bb/write-int32 196608)
      (bb/write-cstring "user")
      (bb/write-cstring user)
      (bb/write-cstring "database")
      (bb/write-cstring database)
      (bb/write-byte 0))))
