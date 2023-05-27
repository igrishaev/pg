(ns pg.client.prot.connection)

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

  (get-password [this])

  (get-user [this])

  (send-sync [this])

  (read-message [this])

  (read-messages [this])

  (read-messages-until [this set-classes])

  (send-message [this message])

  (handle-NoticeResponse [this NoticeResponse])

  (authenticate [this])

  (initiate [this])

  (query [this str-sql opt])

  (terminate [this]))
