(ns pg.client.result
  (:import
   java.util.List
   java.util.Map)
  (:require
   [pg.decode.txt :as txt]))


(def vconj
  (fnil conj []))


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


(defprotocol IResult

  (add-RowDescription [this RowDescription])

  (add-DataRow [this DataRow])

  (add-ErrorResponse [this ErrorResponse])

  (add-CommandComplete [this CommandComplete])

  (complete [this]))


(defrecord Result
    [^Connection connection
     ^Integer index
     ^List list-RowDescription
     ^List list-DataRow
     ^List list-CommandComplete
     ^List list-ErrorResponse]

  IResult

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
               row
               #_DataRow)))

  (add-ErrorResponse [this ErrorResponse]
    (update this :ErrorResponse conj ErrorResponse))

  (add-CommandComplete [this CommandComplete]
    (-> this
        (update :list-CommandComplete conj CommandComplete)))

  (complete [this]

    (cond

      (first list-ErrorResponse)
      (throw (first list-ErrorResponse))

      (zero? index)
      (first list-DataRow)

      (pos? index)
      list-DataRow)))


(defn result [connection]
  (map->Result {:connection connection
                :index -1
                :list-RowDescription []
                :list-DataRow []
                :list-CommandComplete []
                :list-ErrorResponse []}))
