(ns pg.client.prot.message)


(defprotocol IMessage

  (to-bb [this connection])

  (from-bb [this bb connection])

  (handle [this result connection]))


(defmulti tag->message identity)


(defmulti status->message
  (fn [status bb connection]
    status))
