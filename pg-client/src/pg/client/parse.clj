(ns pg.client.parse
  (:import
   [pg.client.message
    AuthenticationOk
    CommandComplete
    AuthenticationKerberosV5
    AuthenticationCleartextPassword
    AuthenticationMD5Password
    AuthenticationSCMCredential
    AuthenticationGSS
    ErrorResponse
    ErrorNode
    RowDescription
    RowColumn
    DataRow
    ParameterStatus
    BackendKeyData
    ReadyForQuery])
  (:require
   [pg.client.bb :as bb]
   [pg.client.message :as message]
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
      (new AuthenticationOk status)

      2
      (new AuthenticationKerberosV5 status)

      3
      (new AuthenticationCleartextPassword status)

      5
      (let [salt
            (bb/read-bytes bb 4)]
        (new AuthenticationMD5Password status salt))

      6
      (new AuthenticationSCMCredential status)

      7
      (new AuthenticationGSS status)

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

    (new ErrorResponse errors)))


(defn- coerce-value [value]
  (case value
    "off" false
    "on"  true
    ""    nil
    value))


(defmethod -parse \S [_ bb]

  (let [param (-> bb bb/read-cstring)
        value (-> bb bb/read-cstring coerce-value)]

    (new ParameterStatus param value)))


(defmethod -parse \K [_ bb]

  (let [pid
        (bb/read-int32 bb)

        secret-key
        (bb/read-int32 bb)]

    (new BackendKeyData pid secret-key)))


(defmethod -parse \Z [_ bb]
  (let [tx-status (char (bb/read-byte bb))]
    (new ReadyForQuery tx-status)))


(defmethod -parse \T [_ bb]

  (let [column-count
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
                         (bb/read-cstring bb)
                         (bb/read-int32 bb)
                         (bb/read-int16 bb)
                         (bb/read-int32 bb)
                         (bb/read-int16 bb)
                         (bb/read-int32 bb)
                         (bb/read-int16 bb))))))]

    (new RowDescription column-count columns)))


(defmethod -parse \D [_ bb]

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

    (new DataRow values)))


(defmethod -parse \C [_ bb]
  (let [tag (bb/read-cstring bb)]
    (new CommandComplete tag)))
