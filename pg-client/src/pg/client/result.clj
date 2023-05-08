(ns pg.client.result
  (:import
   pg.client.connection.Connection)
  (:require
   [pg.client.handle :as handle]
   [pg.client.connection :as connection]))


(defrecord Result
    [^Connection connection])


(defn result [^Connection connection]
  (map->Result {:connection connection}))


(defn fold-messages [result messages]
  (reduce handle/-handle result messages))
