(ns pg.client.result
  (:import
   clojure.lang.ITransientCollection
   java.util.Map
   java.util.HashMap
   java.util.List
   java.util.ArrayList)
  (:require
   [clojure.string :as str]
   [pg.client.coll :as coll]
   [pg.decode.txt :as txt]
   [pg.decode.bin :as bin]
   [pg.client.md5 :as md5]
   [pg.client.conn :as conn]))


(defn transient? [x]
  (instance? ITransientCollection x))


(defn subs-safe
  (^String [^String string from]
   (let [len (.length string)]
     (when (<= from len)
       (.substring string from len))))

  (^String [^String string from to]
   (let [len (.length string)]
     (when (<= from to len)
       (.substring string from to)))))


(defn tag->amount [^String tag]

  (case (subs-safe tag 0 6)

    "INSERT"
    (-> tag
        (subs-safe 7)
        (str/split #" " )
        (second)
        (Long/parseLong))

    ("UPDATE" "DELETE")
    (-> tag (subs-safe 7) Long/parseLong)

    nil))


(defn zip-java-map ^Map [^List the-keys ^List the-vals]
  (let [len
        (.size the-keys)
        res
        (new HashMap)]
    (loop [i 0]
      (if (= i len)
        res
        (let [the-key
              (.get the-keys i)
              the-val
              (.get the-vals i)]
          (.put res the-key the-val)
          (recur (inc i)))))))

;; TODO: refactor this
(defn make-result [phase {:as init :keys [as-java-maps?]}]
  (assoc init
         :I 0
         :fn-keyval
         (cond

           as-java-maps?
           zip-java-map

           :else
           zipmap)

         :phase phase
         :errors (new ArrayList)
         :exceptions (new ArrayList)))


(defn handle-ReadyForQuery
  [result conn {:keys [tx-status]}]
  (conn/set-tx-status conn tx-status)
  result)


(defn handle-BackendKeyData
  [result conn {:keys [pid secret-key]}]
  (conn/set-pid conn pid)
  (conn/set-secret-key conn secret-key)
  result)


(defn handle-ErrorResponse
  [{:as result :keys [^List errors]}
   message]
  (.add errors message)
  result)


(defn handle-ParameterStatus
  [result conn {:keys [param value]}]
  (conn/set-parameter conn param value)
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
  (let [password
        (conn/get-password conn)]
    (conn/send-password conn password))
  result)


(defn handle-Exception
  [{:as result :keys [^List exceptions]} e]
  (.add exceptions e)
  result)


(defn make-subresult
  [result
   {:as RowDescription :keys [columns]}]

  (let [fn-column
        (get result :fn-column keyword)]

    {:RowDescription RowDescription
     :Rows! (transient [])
     :Keys (coll/for-vec [c columns]
             (-> c :name fn-column))}))


(defn make-Keys
  [result
   {:as RowDescription :keys [columns]}]

  (let [fn-column
        (get result :fn-column keyword)]

    (coll/for-vec [c columns]
      (-> c :name fn-column))))


(defn handle-ParameterDescription
  [{:as result :keys [I]}
   ParameterDescription]
  (assoc-in result
            [:map-ParameterDescription I]
            ParameterDescription))


(defn handle-RowDescription
  [{:as result :keys [I]}
   RowDescription]

  (let [Keys
        (make-Keys result RowDescription)

        Rows-init
        (get result :reduce-init (transient []))]

    (-> result
        (assoc-in [:map-RowDescription I] RowDescription)
        (assoc-in [:map-Keys I] Keys)
        (assoc-in [:map-Rows I] Rows-init))))


(defn handle-DataRow
  [{:as result :keys [I fn-keyval]}
   conn
   DataRow]

  (let [reduce-fn
        (get result :reduce-fn conj!)

        encoding
        (conn/get-server-encoding conn)

        RowDescription
        (get-in result [:map-RowDescription I])

        Keys
        (get-in result [:map-Keys I])

        {:keys [^List values]}
        DataRow

        {:keys [^List columns]}
        RowDescription

        ;; TODO: fill opt
        opt
        {}

        ;; TODO: better cycle
        values-decoded
        (coll/doN [i (count values)]

          (let [col
                (.get columns i)

                {:keys [type-oid
                        format]}
                col

                ^bytes buf
                (.get values i)]

            (case (int format)
              0 (let [string (new String buf encoding)]
                  (txt/decode string type-oid opt))
              1 (bin/decode buf type-oid opt))))

        Row
        (fn-keyval Keys values-decoded)]

    (update-in result [:map-Rows I] reduce-fn Row)))


(defn handle-CommandComplete
  [{:as result :keys [I]}
   CommandComplete]
  (-> result
      (assoc-in [:map-CommandComplete I] CommandComplete)
      (update :I inc)))


(defn handle-PortalSuspended
  [result]
  (update result :I inc))


(defn handle [{:as result :keys [phase]}
              conn
              {:as message :keys [msg]}]

  (case msg

    (:AuthenticationOk
     :EmptyQueryResponse
     :CloseComplete
     :BindComplete
     :NoData
     :ParseComplete)
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

    (throw (ex-info "Cannot handle a message"
                    {:phase phase
                     :message message}))))


(defn finalize-query [{:as result
                       :keys [I fn-result]}]

  (loop [i 0
         acc! (transient [])]

    (if (= i I)

      (let [acc
            (cond->> (persistent! acc!)
              fn-result
              (mapv fn-result))]

        (case (count acc)
          0 nil
          1 (first acc)
          acc))

      (let [Rows
            (get-in result [:map-Rows i])

            CommandComplete
            (get-in result [:map-CommandComplete i])

            RowDescription
            (get-in result [:map-RowDescription i])

            {:keys [tag]}
            CommandComplete

            amount
            (some-> tag tag->amount)

            subresult
            (cond

              RowDescription
              (if (transient? Rows)
                (persistent! Rows)
                Rows)

              amount
              amount

              :else
              nil)]

        (recur (inc i) (conj! acc! subresult))))))


(defn finalize-prepare
  [{:keys [statement
           map-ParameterDescription
           map-RowDescription
           I]}]

  {:statement statement
   :RowDescription (get map-RowDescription I)
   :ParameterDescription (get map-ParameterDescription I)})


(defn finalize-errors! [{:keys [^List errors]}]
  (when-not (.isEmpty errors)
    (let [error (.get errors 0)]
      (throw (ex-info "ErrorResponse" {:error error})))))


(defn finalize-exeptions! [{:keys [^List exceptions]}]
  (when-not (.isEmpty exceptions)
    (let [e (.get exceptions 0)]
      (throw e))))


(defn finalize [{:as result :keys [phase]}]

  (finalize-errors! result)
  (finalize-exeptions! result)

  (case phase

    :prepare
    (finalize-prepare result)

    ;; else

    (finalize-query result)))


(defn interact

  ([conn phase]
   (interact conn phase nil))

  ([conn phase init]

   (finalize

    (loop [result (make-result phase init)]

      (let [{:as message :keys [msg]}
            (conn/read-message conn)]

        (let [result
              (try
                (handle result conn message)
                (catch Throwable e
                  (handle-Exception result e)))]

          (if (or (identical? msg :ReadyForQuery)
                  (and (identical? phase :auth)
                       (identical? msg :ErrorResponse)))
            result
            (recur result))))))))
