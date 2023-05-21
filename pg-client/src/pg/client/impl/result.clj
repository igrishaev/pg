(ns pg.client.impl.result
  (:import
   java.util.List
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


(defrecord Result
    [connection
     ^Integer index
     ^List list-RowDescription
     ^List list-DataRow
     ^List list-CommandComplete
     ^List list-ErrorResponse
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
      (let [index (inc index)]
        (-> this
            (update :list-RowDescription conj RowDescription)
            (update :list-DataRow conj [])
            (update :index inc))))

    (add-DataRow [this DataRow]

      (let [RowDescription
            (peek list-RowDescription)

            row
            (decode-row RowDescription DataRow)]

        (update-in this
                   [:list-DataRow index]
                   conj
                   row)))

    (add-ErrorResponse [this ErrorResponse]
      (update this :list-ErrorResponse conj ErrorResponse))

    (add-CommandComplete [this CommandComplete]
      (-> this
          (update :list-CommandComplete conj CommandComplete)))

    (complete [this]

      (let [er (first list-ErrorResponse)]

        (cond

          er
          (throw (ex-info "ErrorResponse" er))

          (zero? index)
          (first list-DataRow)

          (pos? index)
          list-DataRow))))


(defn result [connection]
  (map->Result {:connection connection
                :index -1
                :list-RowDescription []
                :list-DataRow []
                :list-CommandComplete []
                :list-ErrorResponse []
                :-params (new HashMap)}))
