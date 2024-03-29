(ns pg.client.flow
  (:import
   java.io.OutputStream
   java.util.ArrayList
   java.util.HashMap
   java.util.List
   java.util.Map)
  (:require
   [clojure.string :as str]
   [pg.client.as :as as]
   [pg.client.conn :as conn]
   [pg.client.func :as func]
   [pg.client.md5 :as md5]
   [pg.client.msg :as msg]
   [pg.client.scram-sha-256 :as scram-sha-256]
   [pg.coll :as coll]
   [pg.const :as const]
   [pg.decode.bin :as bin]
   [pg.decode.txt :as txt]))


(def default
  {:fn-unify func/unify-idx
   :fn-keyval zipmap
   :fn-column keyword})


(defn tag->amount [^String tag]

  (let [[lead & args]
        (str/split tag #"\s+")]

    (case lead

      "INSERT"
      (-> args (nth 1) Long/parseLong)

      ("UPDATE" "DELETE" "COPY")
      (-> args first Long/parseLong)

      nil)))


(defn remap-as [this]
  (let [as (get this :as as/default)]
    (-> this
        (merge as)
        (dissoc :as))))


(defn make-node []
  (new HashMap))


(defn make-result ^Map [phase init]
  (doto (new HashMap)
    (.putAll default)
    (.putAll (remap-as init))
    (.put :nodes (doto (new ArrayList)
                   (.add (make-node))))
    (.put :I 0)
    (.put :phase phase)
    (.put :errors (new ArrayList))))


(defn handle-ReadyForQuery
  [result conn {:keys [tx-status]}]
  (conn/set-tx-status conn tx-status)
  result)


(defn handle-ErrorResponse
  [{:as result :keys [^List errors]}
   message]
  (.add errors message)
  result)


(defn handle-ParameterStatus
  [result conn {:keys [param value]}]
  (conn/set-parameter conn param value)
  (conn/rebuild-opt conn param value)
  result)


(defn handle-BackendKeyData
  [result conn {:keys [pid secret-key]}]
  (conn/set-pid conn pid)
  (conn/set-secret-key conn secret-key)
  result)


(defn handle-NoticeResponse [result conn NoticeResponse]
  (conn/handle-notice conn NoticeResponse)
  result)


(defn handle-NotificationResponse
  [result conn NotificationResponse]
  (conn/handle-notification conn NotificationResponse)
  result)


(defn handle-NegotiateProtocolVersion
  [result conn {:keys [version params]}]
  (println (format "NotificationResponse, version: %s, params: %s"
                   version params))
  result)


(defn handle-AuthenticationMD5Password
  [result conn {:keys [salt]}]

  (let [user
        (conn/get-user conn)

        password
        (conn/get-password conn)

        hashed
        (md5/hash-password user password salt)]

    (conn/send-password conn hashed))

  result)


(defn handle-AuthenticationCleartextPassword
  [result conn message]
  (let [password (conn/get-password conn)]
    (conn/send-password conn password))
  result)


(defn handle-SCRAM_SHA_256
  [^Map result conn AuthenticationSASL]

  (let [user
        (conn/get-user conn)

        password
        (conn/get-password conn)

        SASL
        (scram-sha-256/step1-client-first-message user password)

        {:keys [client-first-message]}
        SASL

        msg
        (msg/make-SASLInitialResponse const/SCRAM_SHA_256
                                      client-first-message)]

    (conn/send-message conn msg)

    (doto result
      (.put :SASL SASL))))


(defn handle-AuthenticationSASL
  [result conn {:as AuthenticationSASL
                :keys [sasl-types]}]

  (cond

    (contains? sasl-types const/SCRAM_SHA_256)
    (handle-SCRAM_SHA_256 result conn AuthenticationSASL)

    :else
    (let [msg
          (format "None of the SASL authentication types is supported: %s"
                  sasl-types)]
      (throw (ex-info msg {:sasl-types sasl-types})))))


(defn handle-AuthenticationSASLContinue
  [{:as ^Map result :keys [SASL]}
   conn
   {:keys [server-first-message]}]

  (let [SASL
        (-> SASL
            (scram-sha-256/step2-server-first-message server-first-message)
            (scram-sha-256/step3-client-final-message))

        {:keys [client-final-message]}
        SASL

        msg
        (msg/make-SASLResponse client-final-message)]

    (conn/send-message conn msg)

    (doto result
      (.put :SASL SASL))))


(defn handle-AuthenticationSASLFinal
  [{:as ^Map result :keys [SASL]}
   conn
   {:keys [server-final-message]}]

  (-> SASL
      (scram-sha-256/step4-server-final-message server-final-message)
      (scram-sha-256/step5-verify-server-signatures))

  (doto result
    (.remove result :SASL)))


(defn make-Keys
  [{:as result :keys [fn-unify
                      fn-column]}
   {:as RowDescription :keys [columns]}]
  (->> columns
       (mapv :name)
       (fn-unify)
       (mapv fn-column)))


(defn handle-ParameterDescription
  [{:as result :keys [^int I
                      ^List nodes]}
   ParameterDescription]

  (doto ^Map (.get nodes I)
    (.put :ParameterDescription ParameterDescription))

  result)


(defn handle-RowDescription
  [{:as result
    :keys [^int I
           fn-init
           ^List nodes]}
   RowDescription]

  (let [Keys
        (make-Keys result RowDescription)

        Rows-init
        (fn-init)]

    (doto ^Map (.get nodes I)
      (.put :RowDescription RowDescription)
      (.put :Keys Keys)
      (.put :Rows Rows-init))

    result))


(defn handle-DataRow
  [{:as result
    :keys [^int I
           fn-keyval
           fn-reduce
           ^List nodes]}
   conn
   {:keys [^List values
           value-count]}]

  (if (zero? value-count)
    result

    (let [^Map node
          (.get nodes I)

          {:keys [RowDescription
                  Keys
                  Rows]}
          node

          encoding
          (conn/get-server-encoding conn)

          {:keys [^List columns]}
          RowDescription

          opt
          (conn/get-opt conn)

          values-decoded
          (coll/for-n [i (count values)]

            (let [col
                  (.get columns i)

                  {:keys [type-oid
                          format]}
                  col

                  ^bytes buf
                  (.get values i)]

              (when (some? buf)

                (case (int format)
                  0 (let [string (new String buf encoding)]
                      (txt/decode string type-oid opt))
                  1 (bin/decode buf type-oid opt)))))

          Row
          (fn-keyval Keys values-decoded)]

      (.put node :Rows (fn-reduce Rows Row))

      result)))


(defn handle-CommandComplete
  [{:as ^Map result :keys [^int I
                           ^List nodes]}
   CommandComplete]

  (doto ^Map (.get nodes I)
    (.put :CommandComplete CommandComplete))

  (doto nodes
    (.add (make-node)))

  (doto result
    (.put :I (inc I))))


(defn handle-PortalSuspended
  [{:as ^Map result :keys [^int I
                           ^List nodes]}]
  (doto nodes
    (.add (make-node)))

  (doto result
    (.put :I (inc I))))


(defn handle-CopyData
  [{:as result :keys [^OutputStream output-stream]}
   {:keys [^bytes data]}]
  (when output-stream
    (.write output-stream data))
  result)


(defn handle-CopyDone
  [{:as result :keys [^OutputStream output-stream]}
   _]
  (when output-stream
    (.close output-stream))
  result)


(defn handle [{:as result :keys [phase]}
              conn
              {:as message :keys [msg]}]

  (case msg

    (:AuthenticationOk
     :EmptyQueryResponse
     :CloseComplete
     :BindComplete
     :NoData
     :ParseComplete
     :CopyInResponse
     :CopyOutResponse)
    result

    :ErrorResponse
    (handle-ErrorResponse result message)

    :ReadyForQuery
    (handle-ReadyForQuery result conn message)

    :ParameterStatus
    (handle-ParameterStatus result conn message)

    :NoticeResponse
    (handle-NoticeResponse result conn message)

    :NotificationResponse
    (handle-NotificationResponse result conn message)

    :NegotiateProtocolVersion
    (handle-NegotiateProtocolVersion result conn message)

    :AuthenticationMD5Password
    (handle-AuthenticationMD5Password result conn message)

    :AuthenticationCleartextPassword
    (handle-AuthenticationCleartextPassword result conn message)

    :AuthenticationSASL
    (handle-AuthenticationSASL result conn message)

    :AuthenticationSASLContinue
    (handle-AuthenticationSASLContinue result conn message)

    :AuthenticationSASLFinal
    (handle-AuthenticationSASLFinal result conn message)

    :BackendKeyData
    (handle-BackendKeyData result conn message)

    :CommandComplete
    (handle-CommandComplete result message)

    :PortalSuspended
    (handle-PortalSuspended result)

    :RowDescription
    (handle-RowDescription result message)

    :DataRow
    (handle-DataRow result conn message)

    :ParameterDescription
    (handle-ParameterDescription result message)

    :CopyData
    (handle-CopyData result message)

    :CopyDone
    (handle-CopyDone result message)

    ;; else

    (throw (ex-info "Cannot handle a message"
                    {:phase phase
                     :message message}))))


(defn finalize-query [{:keys [^int I
                              fn-result
                              fn-finalize
                              ^List nodes]}]

  (.remove nodes I)

  (let [subs
        (coll/for-seq [^Map node nodes]

          (let [{:keys [Rows
                        CommandComplete
                        RowDescription]}
                node

                {:keys [tag]}
                CommandComplete

                amount
                (some-> tag tag->amount)]

            (cond

              RowDescription
              (if fn-finalize
                (fn-finalize Rows)
                Rows)

              amount
              amount

              :else
              nil)))]

    (let [^List subs
          (if fn-result
            (mapv fn-result subs)
            subs)]

      (case (.size subs)
        0 nil
        1 (.get subs 0)
        subs))))


(defn finalize-prepare
  [{:keys [statement
           I
           ^List nodes]}]

  (let [node
        (.get nodes I)

        {:keys [RowDescription
                ParameterDescription]}
        node]

    {:statement statement
     :RowDescription RowDescription
     :ParameterDescription ParameterDescription}))


(defn finalize-errors! [{:keys [^List errors]}]
  (when-not (.isEmpty errors)
    (let [error (.get errors 0)]
      (throw (ex-info "ErrorResponse" {:error error})))))


(defn finalize [{:as result :keys [phase]}]

  (finalize-errors! result)

  (case phase

    :prepare
    (finalize-prepare result)

    ;; else

    (finalize-query result)))


(defn enough? [phase {:keys [msg]}]
  (or (identical? msg :ReadyForQuery)
      (and (identical? phase :auth)
           (identical? msg :ErrorResponse))))


(defn interact

  ([conn phase]
   (interact conn phase nil))

  ([conn phase init]

   (finalize

    (loop [result (make-result phase init)]

      (let [message
            (conn/read-message conn)

            result
            (handle result conn message)]

        (if (enough? phase message)
          result
          (recur result)))))))
