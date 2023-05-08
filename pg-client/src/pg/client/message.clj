(ns pg.client.message
  (:import
   java.lang.Keyword
   java.nio.ByteBuffer))







(defrecord Result
    [connection
     Rows
     RowDescription
     ])



(defmulti -handle [result message]
  (:tag message))


(defmethod -handle :ErrorResponse
  [{:as result :keys [connection]} {:as message :keys [tag]}]
  (let [response
        (send-message conn response)]
    result))


(defmethod -handle :ErrorResponse
  [{:as result :keys [connection]} {:as message :keys [tag]}]
  (let [response
        (send-message conn response)]
    result))


(defmethod -handle :AuthenticationSASLContinue
  [{:as result :keys [connection]}
   message]
  (let [response
        (send-message conn response)]
    result))



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



(defmulti -parse
  (fn [lead bb]
    lead))


(defrecord ReadyForQuery
    [^Keyword tag
     ^Character tx-status])


(defmethod -parse \Z [_ ^ByteBuffer bb]
  (let [tx-status (char (.get bb))]
    (new ReadyForQuery
         :ReadyForQuery
         tx-status)))


(defrecord BackendKeyData
    [^Keyword tag
     ^Integer pid
     ^Integer secret-key ])


(defmethod -parse \K [_ ^ByteBuffer bb]
  (let [pid (.getInt bb)
        secret-key (.getInt bb)]
    (new BackendKeyData
         :BackendKeyData
         pid
         secret-key)))
