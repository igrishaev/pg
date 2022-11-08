(ns pg.scram
  (:require
   [pg.codec :as codec]
   [clojure.string :as str]))


(defn Hi [^String password ^bytes salt ^Integer iterations]
  )


(defn step1-client-first-message [user password]
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


(defn step2-server-first-message
  [state ^String server-first-message]

  (let [pairs
        (str/split server-first-message #",")

        keyvals
        (into {} (for [pair pairs]
                   (str/split pair #"=" 2)))

        keyvals
        (update keyvals
                "i"
                (fn [x] (Integer/parseInt x)))

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

  ;; channel-binding


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
        (str channel-binding nonce)

        AuthMessage
        (str/join "," [client-first-message-bare
                       server-first-message
                       client-final-message-without-proof])

        SaltedPassword
        ;; (Hi (Normalize password) salt iteration-count)
        1

        ClientKey
        ;; HMAC(SaltedPassword, "Client Key")
        1

        StoredKey
        ;; H(ClientKey)
        1

        ClientSignature
        ;; HMAC(StoredKey, AuthMessage)
        1

        ClientProof
        #_(XOR ClientKey ClientSignature)
        1

        proof
        (-> ClientProof
            codec/b64-encode
            codec/bytes->str)

        client-final-message
        (str client-final-message-without-proof "," proof)]

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




(defn client-first-message ^String [user]

  (let [header
        "n,,"

        nonce
        (str (java.util.UUID/randomUUID))

        ]

    (str header "n=" user ",r=" nonce)))


(defn process-server-first-message [^bytes payload]
  (let [pairs
        (str/split (new String payload) #",")

        keyvals
        (into {} (for [pair pairs]
                   (str/split pair #"=" 2)))

        keyvals
        (update keyvals
                "i"
                (fn [x] (Integer/parseInt x)))

        ;; {salt "s"
        ;;  nonce "r"
        ;;  iters "i"}
        ;; keyvals

        ]

    keyvals
    )

  )
