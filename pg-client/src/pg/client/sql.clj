(ns pg.client.sql)


(defn isolation-level->sql [level]
  (case level

    (:SERIALIZABLE
     :serializable
     "SERIALIZABLE"
     "serializable"
     SERIALIZABLE
     serializable)
    "SERIALIZABLE"

    (:REPEATABLE-READ
     :repeatable-read
     "REPEATABLE-READ"
     "repeatable-read"
     REPEATABLE-READ
     repeatable-read)
    "REPEATABLE READ"

    (:READ-COMMITTED
     :read-committed
     "READ-COMMITTED"
     "read-committed"
     READ-COMMITTED
     read-committed)
    "READ COMMITTED"

    (:READ-UNCOMMITTED
     :read-uncommitted
     "READ-UNCOMMITTED"
     "read-uncommitted"
     READ-UNCOMMITTED
     read-uncommitted)
    "READ UNCOMMITTED"

    (throw
     (ex-info (format "Wrong isolation level: %s" level)
              {:level level}))))


(defn set-tx [{:keys [isolation-level
                      read-only?]}]

  (when (or isolation-level read-only?)
    (with-out-str
      (print "SET TRANSACTION")
      (when isolation-level
        (print " ISOLATION LEVEL ")
        (print (isolation-level->sql isolation-level)))
      (when read-only?
        (print " READ ONLY")))))
