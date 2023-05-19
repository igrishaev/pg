(ns pg.client.auth.md5
  (:require
   [pg.client.bytes :as bytes]
   [pg.client.codec :as codec]
   [pg.client.prot.message :as message]
   [pg.client.prot.connection :as connection]
   [pg.client.prot.result :as result])
  #_
  (:import
   [pg.client.message
    AuthenticationMD5Password
    PasswordMessage]))


#_
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


#_
(defmethod handle/-handle AuthenticationMD5Password
  [result {:keys [salt]}]

  (let [connection
        (result/get-connection result)

        user
        (connection/get-user connection)

        password
        (connection/get-password connection)

        hashed-password
        (hash-password user password salt)

        message
        (new PasswordMessage hashed-password)

        bb
        (message/to-bb message connection)]

    (connection/send-message connection bb))

  result)
