(ns pg.client.md5
  (:require
   [pg.bytes :as bytes]
   [pg.codec :as codec]))


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
