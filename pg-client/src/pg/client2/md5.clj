(ns pg.client2.md5
  (:require
   [pg.client2.bytes :as bytes]
   [pg.client2.codec :as codec]))


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
