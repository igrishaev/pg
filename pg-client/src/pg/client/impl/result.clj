(ns pg.client.impl.result
  (:import
   java.util.List
   java.util.ArrayList
   java.util.Map
   java.util.HashMap)
  (:require
   [pg.client.prot.message :as message]
   [pg.client.prot.result :as result]
   [pg.decode.txt :as txt]))


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


(defn decode-row [RowDescription DataRow]

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
                     (new String ^bytes value "UTF-8")]
                 (txt/-decode type-oid text)))))
         columns
         values)))


(deftype Result
    [connection
     ^Map opt
     ^Integer ^:unsynchronized-mutable index
     ^List list-RowDescription
     ^List list-CommandComplete
     ^List list-ErrorResponse
     ^Map map-results
     ^Map -params
     ^List list-unified-fields]

  result/IResult

  (handle [this messages]
    (result/complete
     (reduce
      (fn [result message]
        (message/handle message result connection))
      this
      messages)))

  (set-parameter [this param value]
    (.put -params param value)
    this)

  (get-parameter [this param]
    (.get -params param))

  (get-connection [this]
    connection)

  (add-RowDescription [this RowDescription]

    (let [{:keys [fn-column]}
          opt]

      (set! index (inc index))
      (.add list-RowDescription RowDescription)
      (.put map-results index (transient []))
      (.add list-unified-fields
            (->> RowDescription
                 (:columns)
                 (mapv :name)
                 (unify-fields)
                 (mapv fn-column))))
    this)

  (add-DataRow [this DataRow]

    (let [RowDescription
          (.get list-RowDescription index)

          fields
          (.get list-unified-fields index)

          values
          (decode-row RowDescription DataRow)

          {:keys [column-count]}
          RowDescription

          {:keys [

                  ;; reduce-fn
                  ;; reduce-val
                  as-vectors?
                  as-maps?
                  as-java-maps?
                  ]}
          opt

          row
          (cond

            as-maps?
            (zipmap fields values)

            as-vectors?
            values

            as-java-maps?
            (doto (new HashMap)
              (.putAll (zipmap fields values)))

            :else
            (zipmap fields values))]

      (conj! (.get map-results index) row))
    this)

  (add-ErrorResponse [this ErrorResponse]
    (.add list-ErrorResponse ErrorResponse)
    this)

  (add-CommandComplete [this CommandComplete]
    (.add list-CommandComplete CommandComplete)
    this)

  (complete [this]

    (let [er (first list-ErrorResponse)]

      (cond

        er
        (throw (ex-info "ErrorResponse" er))

        (zero? index)
        (-> map-results (.get 0) persistent!)

        (pos? index)
        (->> map-results
            (vals)
            (mapv persistent!))))))


(def opt-default
  {:fn-column keyword})


(defn result
  ([connection]
   (result connection nil))

  ([connection opt]
   (new Result
        connection
        (merge opt-default opt)
        -1
        (new ArrayList)
        (new ArrayList)
        (new ArrayList)
        (new HashMap)
        (new HashMap)
        (new ArrayList))))
