(ns pg.client.impl.result
  (:import
   java.util.List
   java.util.ArrayList
   java.util.Map
   java.util.HashMap)
  (:require
   [clojure.string :as str]
   [pg.client.prot.connection :as connection]
   [pg.client.prot.message :as message]
   [pg.client.prot.result :as result]
   [pg.decode.txt :as txt]))


(defn decode-row [RowDescription DataRow ^String encoding]

  (let [{:keys [columns]}
        RowDescription

        {:keys [values]}
        DataRow

        result
        (new ArrayList)]

    (map (fn [column value]

           (let [{:keys [name
                         format
                         type-oid]}
                 column]

             (case (int format)

               0
               (let [text
                     (new String ^bytes value encoding)]
                 (txt/decode text type-oid)))))
         columns
         values)))


(defn unify-fields [fields]

  (let [field->fields
        (group-by identity fields)

        len
        (count fields)]

    (loop [i 0
           result []
           field->idx {}]

      (if (= i len)
        result

        (let [field
              (get fields i)

              fields
              (get field->fields field)]

          (if (= 1 (count fields))

            (recur (inc i)
                   (conj result field)
                   field->idx)

            (let [idx
                  (get field->idx field 0)

                  field'
                  (format "%s_%s" field idx)

                  field->idx'
                  (assoc field->idx field (inc idx))]

              (recur (inc i)
                     (conj result field')
                     field->idx'))))))))


(defn subs-safe
  (^String [^String string from]
   (let [len (.length string)]
     (when (<= from len)
       (.substring string from len))))

  (^String [^String string from to]
   (let [len (.length string)]
     (when (<= from to len)
       (.substring string from to)))))


(defn parse-tag [^String tag]

  (when-let [command
             (subs-safe tag 0 6)]

    (case command

      "INSERT"
      (-> tag
          (subs-safe 7)
          (str/split #" " )
          (second)
          (Long/parseLong))


      ("SELECT" "UPDATE" "DELETE")
      (-> tag (subs-safe 7) Long/parseLong)

      nil)))


(deftype Frame
    [;; init
     -connection
     ^Map  -opts
     ^List -rows
     ;; state
     ^Map  ^:unsynchronized-mutable -RowDescription
     ^Map  ^:unsynchronized-mutable -CommandComplete
     ^List ^:unsynchronized-mutable -fields]

  result/IResult

  (add-RowDescription [this RowDescription]

    (set! -RowDescription RowDescription)

    (let [{:keys [fn-column]}
          -opts

          fields
          (->> RowDescription
               (:columns)
               (mapv :name)
               (unify-fields)
               (mapv fn-column))]

      (set! -fields fields)))

  (add-CommandComplete [this CommandComplete]
    (set! -CommandComplete CommandComplete))

  (add-DataRow [this DataRow]

    (let [encoding
          (connection/get-server-encoding -connection)

          values
          (decode-row -RowDescription DataRow encoding)

          {:keys [column-count]}
          -RowDescription

          {:keys [as-vectors?
                  as-maps?
                  as-java-maps?]}
          -opts

          row
          (cond

            as-maps?
            (zipmap -fields values)

            as-vectors?
            values

            as-java-maps?
            (doto (new HashMap)
              (.putAll (zipmap -fields values)))

            :else
            (zipmap -fields values))]

      (conj! -rows row)))

  (complete [this]

    (let [{:keys [tag]}
          -CommandComplete

          {:keys [fn-result]}
          -opts]

      (cond

        -RowDescription
        (cond-> (persistent! -rows)
          fn-result
          fn-result)

        tag
        (parse-tag tag)))))


(defn make-frame [connection opt]
  (new Frame connection opt (transient []) nil nil nil))


(defn afirst [^List a]
  (when (-> a .size (> 0))
    (.get a 0)))


(deftype Result
    [-connection
     ^Map  -opts
     ^Frame ^:unsynchronized-mutable -frame
     ^List -frames
     ^List -list-ErrorResponse
     ^List -list-Exception
     ^Map  -ex-data]

  result/IResult

  (add-Exception [this e]
    (.add -list-Exception e)
    this)

  (handle [this messages]
    (result/complete
     (reduce
      (fn [result message]
        (try
          (message/handle message result -connection)
          (catch Throwable e
            (result/add-Exception this e))))
      this
      messages)))

  (get-connection [this]
    -connection)

  (add-RowDescription [this RowDescription]
    (result/add-RowDescription -frame RowDescription))

  (add-DataRow [this DataRow]
    (result/add-DataRow -frame DataRow))

  (add-ErrorResponse [this ErrorResponse]
    (.add -list-ErrorResponse ErrorResponse))

  (add-CommandComplete [this CommandComplete]
    (result/add-CommandComplete -frame CommandComplete)
    (.add -frames -frame)
    (set! -frame (make-frame -connection -opts)))

  (complete [this]

    (when-let [ex (afirst -list-Exception)]
      (throw ex))

    (when-let [{:keys [errors]}
               (afirst -list-ErrorResponse)]
      (throw (ex-info "ErrorResponse"
                      {:errors errors
                       :details -ex-data})))

    (let [results
          (mapv result/complete -frames)]

      (case (count results)

        0 nil

        1 (get results 0)

        results))))


(def opt-default
  {:fn-column keyword})


(defn make-result
  ([connection]
   (make-result connection nil))

  ([connection opt]
   (make-result connection opt nil))

  ([connection opt data]
   (let [opt
         (merge opt-default opt)]

     (new Result
          connection
          opt
          (make-frame connection opt)
          (new ArrayList)
          (new ArrayList)
          (new ArrayList)
          data))))
