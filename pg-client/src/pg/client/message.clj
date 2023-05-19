(ns pg.client.message
  (:import
   java.util.Set
   java.util.List
   clojure.lang.Keyword
   java.nio.ByteBuffer)
  (:require
   [pg.client.proto.message :as message]
   [pg.client.codec :as codec]
   [pg.client.bb :as bb]))


(defrecord AuthenticationOk
    [^Integer status])


(defrecord AuthenticationKerberosV5
    [^Integer status])


(defrecord AuthenticationCleartextPassword
    [^Integer status])


(defrecord AuthenticationMD5Password
    [^Integer status
     ^bytes salt])


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


(defrecord AuthenticationSASL
    [^Integer status
     ^Set sasl-types])


(defrecord AuthenticationSASLContinue
    [^Integer status
     ^String server-first-message])


(defrecord AuthenticationSASLFinal
    [^Integer status
     ^String server-final-message])


(defrecord ReadyForQuery
    [^Character tx-status])


(defrecord BackendKeyData
    [^Integer pid
     ^Integer secret-key])


(defrecord RowDescription
    [^Integer column-count
     ^List columns])


(defrecord ParameterStatus
    [^String param
     ^Object value])


(defrecord ErrorNode
    [^Character tag
     ^String message])


(defrecord ErrorResponse
    [^List errors])


(defrecord NoticeResponse
    [^List messages])


(defrecord EmptyQueryResponse
    [])


(defrecord DataRow
    [^List values])


(defrecord NoData [])


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
     ^List columns])


(defrecord CommandComplete
    [^String tag])


(defrecord PasswordMessage
    [^String password]

  message/IMessage

  (to-bb [this]
    (let [len
          (+ 4 (codec/bytes-count password) 1)]
      (doto (bb/allocate (inc len))
        (bb/write-byte \p)
        (bb/write-int32 len)
        (bb/write-cstring password)))))
