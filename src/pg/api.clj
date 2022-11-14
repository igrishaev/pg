(ns pg.api
  "
  Public client API.
  "
  )


(defn query []
  )


(defn insert []
  )


(defn insert-batch []
  )


(defn update []
  )


(defn delete []
  )


(defn prepare
  ([conn stmt-name query]
   (prepare conn stmt-name query nil))

  ([conn stmt-name query oid-types]

   )
  )


(defmacro with-transaction []
  )


(defmacro with-statement []
  )


(defmacro with-connection []
  )


(defn copy-in []
  )


(defn copy-out []
  )


(defn func-call []
  )


(defn notify []
  )


(defn cancell []
  )


(defn close-statement []
  )


(defn terminate []
  )


(defn get-isolation-level []
  )


(defn set-isolation-level []
  )


(defn sync []
  )


(defn flush []
  )


(defn reducible-query []
  )


(defn get-by-id []
  )


(defn find-by-keys []
  )


(defn find-one-by-keys []
  )


(defn component []
  )
