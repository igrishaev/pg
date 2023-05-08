(ns pg.client.result
  (:import
   java.util.List
   java.util.Map
   pg.client.connection.Connection)
  (:require
   [pg.client.connection :as connection]))


(def vconj
  (fnil conj []))


(defprotocol IResult

  (add-RowDescription [this RowDescription])

  (add-DataRow [this DataRow])

  (add-CommandComplete [this CommandComplete]))


(defrecord Result
    [^Connection connection
     ^Integer result-count
     ^Map index->RowDescription
     ^Map index->DataRow
     ^Map index->CommandComplete]

  IResult

  (add-RowDescription [this RowDescription]
    (let [index (inc result-count)]
      (-> this
          (update-in [:index->RowDescription index]
                     update
                     vconj
                     RowDescription)
          (assoc :result-count index))))

  (add-DataRow [this DataRow]
    (update-in this
               [:index->RowDescription result-count]
               update
               vconj
               DataRow))

  (add-CommandComplete [this CommandComplete]
    (update-in this
               [:index->CommandComplete result-count]
               update
               vconj
               CommandComplete)))


(defn result [^Connection connection]
  (map->Result {:connection connection
                :result-count 0
                :index->RowDescription {}
                :index->DataRow {}
                :index->CommandComplete {}}))
