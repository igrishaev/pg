(ns pg.client.proto.result)

(defprotocol IResult

  (get-connection [this])

  (add-RowDescription [this RowDescription])

  (add-DataRow [this DataRow])

  (add-ErrorResponse [this ErrorResponse])

  (add-CommandComplete [this CommandComplete])

  (complete [this]))
