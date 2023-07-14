(ns pg.client.scram-sha-256
  "
  https://postgrespro.ru/docs/postgresql/14/sasl-authentication
  https://www.rfc-editor.org/rfc/rfc7677
  https://www.rfc-editor.org/rfc/rfc5802
  https://gist.github.com/jkatz/e0a1f52f66fa03b732945f6eb94d9c21
  "
  (:require
   [pg.client.bytes :as bytes]
   [pg.client.codec :as codec]
   [clojure.string :as str])
  (:import
   java.util.UUID))


(defn parse-int [x]
  (Integer/parseInt x))


(defn Hi ^bytes
  [^bytes secret ^bytes message ^Integer iterations]

  (loop [i 0
         msg (bytes/concat message (byte-array [0 0 0 1]))
         u (byte-array 32)]

    (if (= i iterations)
      u
      (let [u-next
            (codec/hmac-sha-256 secret msg)]
        (recur (inc i)
               u-next
               (bytes/xor u u-next))))))


(defn H ^bytes [^bytes input]
  (codec/sha-256 input))


(defn parse-message [^String message]
  (let [pairs
        (str/split message #",")]
    (into {} (for [pair pairs] ;; TODO: for
               (str/split pair #"=" 2)))))


(defn step1-client-first-message
  [^String user ^String password]

  (let [gs2-header
        "n,,"

        nonce
        (str (UUID/randomUUID))

        client-first-message-bare
        (str "n=" user ",r=" nonce)

        client-first-message
        (str gs2-header client-first-message-bare)]

    {:user user
     :password password
     :gs2-header gs2-header
     :client-first-message client-first-message
     :client-first-message-bare client-first-message-bare}))


(defn step2-server-first-message

  ;; r=68591664-4b88-44d8-86ff-e25d20cff4bb48uf9I3XRSTkMOpLWY3LeZOL,s=MXf1hERKrJWAQSlcYSRe6A==,i=4096

  [state ^String server-first-message]

  (let [pairs
        (str/split server-first-message #",")

        keyvals
        (into {} (for [pair pairs] ;; TODO: for
                   (str/split pair #"=" 2)))

        keyvals
        (update keyvals "i" parse-int)

        {salt-encoded "s"
         nonce "r"
         iteration-count "i"}
        keyvals

        salt
        (-> salt-encoded
            codec/str->bytes
            codec/b64-decode)]

    (assoc state
           :salt salt
           :nonce nonce
           :iteration-count iteration-count
           :server-first-message server-first-message)))


(defn step3-client-final-message [state]

  (let [{:keys [salt
                nonce
                password
                gs2-header
                iteration-count
                server-first-message
                client-first-message-bare]}
        state

        channel-binding
        (-> gs2-header
            codec/str->bytes
            codec/b64-encode
            codec/bytes->str)

        client-final-message-without-proof
        (str "c=" channel-binding ",r=" nonce)

        AuthMessage
        (str/join "," [client-first-message-bare
                       server-first-message
                       client-final-message-without-proof])

        SaltedPassword
        (Hi (-> password
                codec/normalize-nfc
                codec/str->bytes)
            salt
            iteration-count)

        ClientKey
        (codec/hmac-sha-256 SaltedPassword (codec/str->bytes "Client Key"))

        StoredKey
        (H ClientKey)

        ClientSignature
        (codec/hmac-sha-256 StoredKey (codec/str->bytes AuthMessage))

        ClientProof
        (bytes/xor ClientKey ClientSignature)

        ServerKey
        (codec/hmac-sha-256 SaltedPassword (codec/str->bytes "Server Key"))

        ServerSignature
        (codec/hmac-sha-256 ServerKey (codec/str->bytes AuthMessage))

        proof
        (-> ClientProof
            codec/b64-encode
            codec/bytes->str)

        client-final-message
        (str client-final-message-without-proof ",p=" proof)]

    (assoc state
           :proof proof
           :ClientProof ClientProof
           :ServerKey ServerKey
           :ServerSignature ServerSignature
           :channel-binding channel-binding
           :client-final-message-without-proof client-final-message-without-proof
           :client-final-message client-final-message)))


(defn step4-server-final-message

  ;; "v=dinCviSchyXpv0W3JPXaT3QYUotxzTWPL8Mw103bRbM="

  [state ^String server-final-message]

  (let [{:keys [ServerSignature]}
        state

        {verifier "v"}
        (parse-message server-final-message)

        ServerSignature2
        (-> verifier
            codec/str->bytes
            codec/b64-decode)]

    (assoc state
           :server-final-message server-final-message
           :ServerSignature2 ServerSignature2)))


(defn step5-verify-server-signatures
  [{:as state :keys [ServerSignature ServerSignature2]}]
  (if (bytes/== ServerSignature ServerSignature2)
    state
    (throw (ex-info
            "Server signatures do not match"
            {:state (dissoc state :password)}))))
