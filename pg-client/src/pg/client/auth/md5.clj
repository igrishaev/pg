(ns pg.client.auth.md5
  (:require
   pg.client.message
   [pg.client.handle :as handle]
   [pg.client.bytes :as bytes]
   [pg.client.codec :as codec]
   [pg.client.proto.message :as message]
   [pg.client.proto.connection :as connection]
   [pg.client.proto.result :as result])
  (:import
   [pg.client.message
    AuthenticationMD5Password
    PasswordMessage]))


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
        (message/to-bb message)]

    (connection/send-message connection bb))

  result)
