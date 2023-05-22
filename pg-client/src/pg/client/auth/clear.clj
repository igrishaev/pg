(ns pg.client.auth.clear
  (:require
   [pg.client.bb :as bb]
   [pg.client.impl.message]
   [pg.client.prot.connection :as connection]
   [pg.client.prot.message :as message])
  (:import
   pg.client.impl.message.PasswordMessage))


(defrecord AuthenticationCleartextPassword
    [^Integer status]

  message/IMessage

  (handle [this result connection]

    (let [password
          (connection/get-password connection)

          message
          (new PasswordMessage password)]

      (connection/send-message connection message))

    result))


(defmethod message/status->message 3
  [status bb connection]
  (new AuthenticationCleartextPassword status))
