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


(defn decode-row [RowDescription DataRow]

  (let [{:keys [column-count
                columns]}
        RowDescription

        {:keys [values]}
        DataRow]

    (loop [i 0
           res {}]

      (if (= i column-count)
        res

        (let [column
              (get columns i)

              value
              (get values i)

              {:keys [name
                      format
                      type-oid]}
              column

              decoded
              (case (int format)

                0
                (let [text
                      (new String ^bytes value "UTF-8")]
                  (txt/-decode type-oid text)))

              res'
              (assoc res name decoded)]

          (recur (inc i) res'))))))


(deftype Result
    [connection
     ^Integer ^:unsynchronized-mutable index
     ^List list-RowDescription
     ^List list-CommandComplete
     ^List list-ErrorResponse
     ^Map map-results
     ^Map -params]

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
    (set! index (inc index))
    (.add list-RowDescription RowDescription)
    (.put map-results index (transient []))
    this)

  (add-DataRow [this DataRow]
    (let [RowDescription
          (.get list-RowDescription index)
          row
          (decode-row RowDescription DataRow)]
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


(defn result [connection]
  (new Result
       connection
       -1
       (new ArrayList)
       (new ArrayList)
       (new ArrayList)
       (new HashMap)
       (new HashMap)))
