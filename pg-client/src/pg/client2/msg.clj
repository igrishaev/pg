(ns pg.client2.msg
  (:import
   clojure.lang.Keyword
   java.util.Map
   java.io.ByteArrayOutputStream)
  (:require
   [pg.client2.out :as out]
   [pg.client2.bb :as bb]
   [pg.client2.coll :as coll]))


(defmacro get-server-encoding [opt]
  `(or (get ~opt :server-encoding) "UTF-8"))


(defmacro get-client-encoding [opt]
  `(or (get ~opt :client-encoding) "UTF-8"))


(defn parse-AuthenticationOk [bb opt]
  {:msg :AuthenticationOk
   :status 0})


(defn parse-AuthenticationResponse [bb opt]

  (let [status (bb/read-int32 bb)]

    (case status

      0
      (parse-AuthenticationOk bb opt)

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


(defn parse-DataRow [bb opt]

  (let [value-count
        (bb/read-int16 bb)

        values
        (coll/doN [i value-count]
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
        (coll/doN [i column-count]
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


(defn parse-message

  [tag bb opt]

  (case tag

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

    ;; else

    (throw (ex-info "Cannot parse a message"
                    {:tag tag
                     :opt opt
                     :bb (bb/to-vector bb)}))))


(defn to-bb
  [^Character c ^ByteArrayOutputStream out]

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
      (bb/write-bytes buf))))


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

    (doseq [[^String k ^String v] options]
      (doto out
        (out/write-cstring k encoding)
        (out/write-cstring v encoding)))

    (doto out
      (out/write-byte 0))

    (to-bb nil out)))


(defn make-Close [source-type source]
  {:msg :Close
   :source-type source-type
   :source source})


(defn encode-Close
  [{:keys [^Character source-type
           ^String source]}
   opt]

  (let [^String encoding
        (get-client-encoding opt)

        out
        (doto (out/create)
          (out/write-char source-type)
          (out/write-cstring source))]

    (to-bb \C out)))


(defn make-Query [query]
  {:msg :Query
   :query query})


(defn encode-Query
  [{:keys [^String query]} opt]

  (let [^String encoding
        (get-client-encoding opt)

        out
        (doto (out/create)
          (out/write-cstring query))]

    (to-bb \Q out)))


(defn encode-message [{:as message :keys [msg]} opt]

  (case msg

    :Query
    (encode-Query message opt)

    :Close
    (encode-Close message opt)

    :StartupMessage
    (encode-StartupMessage message opt)

    ;; else

    (throw (ex-info "Cannot encode a message"
                    {:opt opt
                     :message message}))))
