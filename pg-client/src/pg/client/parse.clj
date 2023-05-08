(ns pg.client.parse
  (:import
   [pg.client.message
    AuthenticationOk
    AuthenticationKerberosV5
    AuthenticationCleartextPassword
    AuthenticationMD5Password
    AuthenticationSCMCredential
    AuthenticationGSS
    ErrorResponse
    ErrorNode
    ParameterStatus
    BackendKeyData
    ReadyForQuery])
  (:require
   [pg.client.message :as message]
   [pg.client.bb :as bb]
   [pg.error :as e]))


(defmulti -parse
  (fn [lead bb]
    (char lead)))


(defmethod -parse :default
  [^Character tag bb]
  (e/error! "Cannot parse a message, tag: %s" tag))


(defmethod -parse \R [_ bb]

  (let [status (int (bb/read-int32 bb))]

    (case status

      0
      (new AuthenticationOk :AuthenticationOk status)

      2
      (new AuthenticationKerberosV5 :AuthenticationKerberosV5 status)

      3
      (new AuthenticationCleartextPassword :AuthenticationCleartextPassword status)

      5
      (let [salt
            (bb/read-bytes bb 4)]
        (new AuthenticationMD5Password :AuthenticationMD5Password status salt))

      6
      (new AuthenticationSCMCredential :AuthenticationSCMCredential status)

      7
      (new AuthenticationGSS :AuthenticationGSS status)

      ;; 8
      ;; {:type :AuthenticationGSSContinue
      ;;  :status status
      ;;  :auth (bb/read-rest bb)}

      9
      {:type :AuthenticationSSPI
       :status status}

      ;; 10
      ;; {:type :AuthenticationSASL
      ;;  :status status
      ;;  :sasl-types
      ;;  (loop [acc #{}]
      ;;    (let [item
      ;;          (-> bb
      ;;              bb/read-cstring
      ;;              codec/bytes->str)]
      ;;      (if (= item "")
      ;;        acc
      ;;        (recur (conj acc item)))))}

      ;; 11
      ;; {:type :AuthenticationSASLContinue
      ;;  :status status
      ;;  :server-first-message
      ;;  (-> bb
      ;;      bb/read-rest
      ;;      codec/bytes->str)}

      ;; 12
      ;; {:type :AuthenticationSASLFinal
      ;;  :status status
      ;;  :server-final-message
      ;;  (-> bb
      ;;      bb/read-rest
      ;;      codec/bytes->str)}

      ;; else

      (e/error! "Unknown authentication message, status: %s" status

                #_
                {:status status
                 :bb bb
                 :in ::here}))
))


(defmethod -parse \E [_ bb]

  (let [errors
        (loop [acc []]
          (let [field-tag (bb/read-byte bb)]
            (if (zero? field-tag)
              acc
              (let [field-text
                    (bb/read-cstring bb)

                    error
                    (new ErrorNode
                         (char field-tag)
                         field-text)]

                (recur (conj acc error))))))]

    (new ErrorResponse :ErrorResponse errors)))


(defn- coerce-value [value]
  (case value
    "off" false
    "on"  true
    ""    nil
    value))


(defmethod -parse \S [_ bb]

  (let [param (-> bb bb/read-cstring)
        value (-> bb bb/read-cstring coerce-value)]

    (new ParameterStatus :ParameterStatus param value)))


(defmethod -parse \K [_ bb]

  (let [pid
        (bb/read-int32 bb)

        secret-key
        (bb/read-int32 bb)]

    (new BackendKeyData :BackendKeyData pid secret-key)))


(defmethod -parse \Z [_ bb]
  (let [tx-status (char (bb/read-byte bb))]
    (new ReadyForQuery :ReadyForQuery tx-status)))
