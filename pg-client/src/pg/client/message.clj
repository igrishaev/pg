(ns pg.client.message
  (:import
   java.util.Set
   java.util.List
   clojure.lang.Keyword
   java.nio.ByteBuffer))


(defprotocol IMessage
  (--parse [this bb])
  )


(defrecord AuthenticationOk
    [^Keyword tag
     ^Integer status])


(defrecord AuthenticationKerberosV5
    [^Keyword tag
     ^Integer status])


(defrecord AuthenticationCleartextPassword
    [^Keyword tag
     ^Integer status])


(defrecord AuthenticationMD5Password
    [^Keyword tag
     ^Integer status
     ^bytes salt])


(defrecord AuthenticationSCMCredential
    [^Keyword tag
     ^Integer status])


(defrecord AuthenticationGSS
    [^Keyword tag
     ^Integer status])


(defrecord AuthenticationGSSContinue
    [^Keyword tag
     ^Integer status
     ^bytes auth])


(defrecord AuthenticationSSPI
    [^Keyword tag
     ^Integer status])


(defrecord AuthenticationSASL
    [^Keyword tag
     ^Integer status
     ^Set sasl-types])


(defrecord AuthenticationSASL
    [^Keyword tag
     ^Integer status
     ^Set sasl-types])


(defrecord AuthenticationSASLContinue
    [^Keyword tag
     ^Integer status
     ^String server-first-message])


(defrecord AuthenticationSASLFinal
    [^Keyword tag
     ^Integer status
     ^String server-final-message])


(defrecord ReadyForQuery
    [^Keyword tag
     ^Character tx-status])


(defrecord BackendKeyData
    [^Keyword tag
     ^Integer pid
     ^Integer secret-key])


(defrecord RowDescription
    [^Keyword tag
     ^Integer column-count
     ^List columns])


(defrecord ParameterStatus
    [^Keyword tag
     ^String param
     ^Object value])


(defrecord ErrorNode
    [^Character tag
     ^String message])


(defrecord ErrorResponse
    [^Keyword tag
     ^List errors])


(defrecord NoticeResponse
    [^Keyword tag
     ^List messages])


(defrecord EmptyQueryResponse
    [^Keyword tag])


(defrecord DataRow
    [^Keyword tag
     ^List columns])


(defrecord NoData
    [^Keyword tag])


(defn ready-for-query? [message]
  (instance? ReadyForQuery message))








(defrecord Result
    [connection

     Rows
     RowDescription])



(defmulti -handle
  (fn [result message]
    (:tag message)))


(defmethod -handle :ErrorResponse
  [{:as result :keys [connection]}
   {:as message :keys [tag]}]
  #_
  (let [response
        (send-message conn response)]
    result))


(defmethod -handle :ErrorResponse
  [{:as result :keys [connection]} {:as message :keys [tag]}]
  #_
  (let [response
        (send-message conn response)]
    result))


(defmethod -handle :AuthenticationSASLContinue
  [{:as result :keys [connection]}
   message]
  #_
  (let [response
        (send-message conn response)]
    result))



#_
(defn foobar [{:as result :keys [connection]}
              {:as message :keys [tag]}]

  (case tag

    :ErrorResponse
    (assoc result :ErrorResponse message)

    :RowDescription
    (assoc result
           :Rows (transient [])
           :RowDescription message)

    :DataRow
    (let [row (decode-row RowDescription message)]
      (update result :Rows conj! row)))



  )
