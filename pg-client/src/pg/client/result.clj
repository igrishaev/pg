(ns pg.client.result
  (:import
   java.util.List
   java.util.Map))


(def vconj
  (fnil conj []))


(defprotocol IResult

  (add-RowDescription [this RowDescription])

  (add-DataRow [this DataRow])

  (add-CommandComplete [this CommandComplete]))


(defrecord Result
    [^Connection connection
     ^Integer index
     ^List list-RowDescription
     ^List list-DataRow
     ^List list-CommandComplete]

  IResult

  (add-RowDescription [this RowDescription]
    (let [index (inc index)]
      (-> this
          (update :list-RowDescription conj RowDescription)
          (update :list-DataRow conj [])
          (update :index inc))))

  (add-DataRow [this DataRow]
    (update-in this
               [:list-DataRow index]
               conj
               DataRow))

  (add-CommandComplete [this CommandComplete]
    (-> this
        (update :list-CommandComplete conj CommandComplete))))


(defn result [connection]
  (map->Result {:connection connection
                :index -1
                :list-RowDescription []
                :list-DataRow []
                :list-CommandComplete []}))
