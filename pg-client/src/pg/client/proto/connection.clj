(ns pg.client.proto.connection)

(defprotocol IConnection

  (set-pid [this pid])

  (get-pid [this])

  (set-secret-key [this secret-key])

  (get-secret-key [this])

  (set-tx-status [this tx-status])

  (get-tx-status [this])

  (set-parameter [this param value])

  (get-server-encoding [this])

  (get-client-encoding ^String [this])

  (get-parameter [this param])

  (write-message [this items])

  (get-password [this])

  (get-user [this])

  (read-message [this])

  (read-messages [this])

  (read-messages-until [this set-classes])

  (send-message [this bb])

  (authenticate [this])

  (initiate [this])

  (query [this str-sql]))
