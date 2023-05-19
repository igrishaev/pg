(ns pg.client.proto.message)

(defprotocol IMessage
  (to-bb [this connection])
  (from-bb [this bb connection]))
