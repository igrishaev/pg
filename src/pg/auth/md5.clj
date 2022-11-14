(ns pg.auth.md5
  (:require
   [pg.bytes :as b]
   [pg.codec :as codec]))


(defn hash-password ^String
  [^String user ^String password ^bytes salt]

  (let [creds
        (-> (str password user)
            codec/str->bytes
            codec/md5
            codec/bytes->hex
            codec/str->bytes)]

    (->> (b/concat creds salt)
         codec/md5
         codec/bytes->hex
         (str "md5"))))
