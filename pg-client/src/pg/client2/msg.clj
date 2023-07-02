(ns pg.client2.msg
  (:import
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


(defn parse-message

  [tag bb opt]

  (case tag

    \C
    (parse-CommandComplete bb opt)

    \D
    (parse-DataRow bb opt)

    \T
    (parse-RowDescription bb opt)

    ))


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
      (bb/write-int32 buf-len)
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
           options]}
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

))
