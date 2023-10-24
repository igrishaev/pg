(ns pg.honey
  (:require
   [pg.client :as pg]
   [honey.sql :as sql]))


(def HONEY_OVERRIDES
  {:numbered true})


(defn sql-format

  ([sql-map]
   (sql-format sql-map nil))

  ([sql-map opt]
   (sql/format sql-map (merge opt HONEY_OVERRIDES))))


(defn query
  "
  Like `pg.client/query` but accepts a HoneySQL map
  which gets rendered into a SQL string.

  Arguments:
  - conn: a Connection object;
  - sql-map: a map like {:select [:this :that] :from [...]}
  - opt: a mixture of `pg.client/query` and HoneySQL parameters.

  Result:
  - same as `pg.client/query`.
  "

  ([conn sql-map]
   (query conn sql-map nil))

  ([conn sql-map opt]
   (let [[sql]
         (sql-format sql-map opt)]
     (pg/query conn sql opt))))


(defn execute
  "
  Like `pg.client/execute` but accepts a HoneySQL map
  which gets rendered into SQL vector and split on a query
  and parameters.

  Arguments:
  - conn: a Connection object;
  - sql-map: a map like {:select [:this :that] :from [...]}
  - opt: a mixture of `pg.client/execute` and HoneySQL parameters.

  Result:
  - same as `pg.client/execute`.
  "

  ([conn sql-map]
   (execute conn sql-map nil))

  ([conn sql-map opt]
   (let [[sql & params]
         (sql-format sql-map opt)]
     (pg/execute conn
                 sql
                 params
                 opt))))
