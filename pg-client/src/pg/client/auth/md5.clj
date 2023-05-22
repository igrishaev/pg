(ns pg.client.auth.md5
  (:require
   [pg.client.bb :as bb]
   [pg.client.bytes :as bytes]
   [pg.client.codec :as codec]
   [pg.client.impl.message]
   [pg.client.prot.connection :as connection]
   [pg.client.prot.message :as message])
  (:import
   pg.client.impl.message.PasswordMessage))


(defn hash-password ^String
  [^String user ^String password ^bytes salt]

  (let [^bytes creds
        (-> (str password user)
            codec/str->bytes
            codec/md5
            codec/bytes->hex
            codec/str->bytes)]

    (->> (bytes/concat creds salt)
         (codec/md5)
         (codec/bytes->hex)
         (str "md5"))))


(defrecord AuthenticationMD5Password
    [^Integer status
     ^bytes salt]

    message/IMessage

    (handle [this result connection]

      (let [user
            (connection/get-user connection)

            password
            (connection/get-password connection)

            hashed-password
            (hash-password user password salt)

            message
            (new PasswordMessage hashed-password)]

        (connection/send-message connection message))

      result))


(defmethod message/status->message 5
  [status bb connection]
  (let [salt (bb/read-bytes bb 4)]
    (new AuthenticationMD5Password status salt)))
