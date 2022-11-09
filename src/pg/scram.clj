(ns pg.scram
  "
  https://postgrespro.ru/docs/postgresql/14/sasl-authentication
  https://www.rfc-editor.org/rfc/rfc7677
  https://www.rfc-editor.org/rfc/rfc5802
  https://gist.github.com/jkatz/e0a1f52f66fa03b732945f6eb94d9c21
  "
  (:require
   [pg.codec :as codec]
   [clojure.string :as str]))


(defn Hi ^bytes
  [^String password ^bytes salt ^Integer iterations]

  (let [message
        (codec/str->bytes password)

        salt-init
        (codec/concat-bytes salt (byte-array [0 0 0 1]))]

    (loop [i 0
           u (codec/hmac-sha-256 message salt-init)]
      (if (= i iterations)
        u
        (recur
         (inc i)
         (codec/xor-bytes u (codec/hmac-sha-256 message u)))))))


(defn H ^bytes [^bytes input]
  (codec/sha-256 input))


(defn step1-client-first-message
  [^String user ^String password]

  (let [gs2-header
        "n,,"

        nonce
        (str (java.util.UUID/randomUUID))

        client-first-message-bare
        (str "n=" user ",r=" nonce)

        client-first-message
        (str gs2-header client-first-message-bare)]

    {:user user
     :password password
     :gs2-header gs2-header
     :client-first-message client-first-message
     :client-first-message-bare client-first-message-bare}))


(defn parse-int [x]
  (Integer/parseInt x))


(defn step2-server-first-message
  [state ^String server-first-message]

  (let [pairs
        (str/split server-first-message #",")

        keyvals
        (into {} (for [pair pairs]
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
        (Hi (codec/normalize-nfc password) salt iteration-count)

        ClientKey
        (codec/hmac-sha-256 SaltedPassword (codec/str->bytes "Client Key"))

        StoredKey
        (H ClientKey)

        ClientSignature
        (codec/hmac-sha-256 StoredKey (codec/str->bytes AuthMessage))

        ClientProof
        (codec/xor-bytes ClientKey ClientSignature)

        proof
        (-> ClientProof
            codec/b64-encode
            codec/bytes->str)

        client-final-message
        (str client-final-message-without-proof ",p=" proof)]

    (assoc state
           :proof proof
           :ClientProof ClientProof
           :channel-binding channel-binding
           :client-final-message-without-proof client-final-message-without-proof
           :client-final-message client-final-message)))


(defn step4-server-final-message [state server-final-message]

  (let [
]

    (assoc state)))


(defn step5-verify [state]

  (let [
]

    (assoc state)))
