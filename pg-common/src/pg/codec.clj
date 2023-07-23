(ns pg.codec
  (:import
   java.text.Normalizer
   java.text.Normalizer$Form
   javax.crypto.Mac
   javax.crypto.spec.SecretKeySpec
   java.security.MessageDigest
   java.util.Base64))


(def ^:const UTF8 "UTF-8")


(defn b64-decode ^bytes [^bytes input]
  (.decode (Base64/getDecoder) input))


(defn b64-encode ^bytes [^bytes input]
  (.encode (Base64/getEncoder) input))


(defn bytes->str
  (^String [^bytes input]
   (new String input UTF8))

  (^String [^bytes input ^String encoding]
   (new String input encoding)))


(defn str->bytes
  (^bytes [^String input]
   (.getBytes input UTF8))

  (^bytes [^String input ^String encoding]
   (.getBytes input encoding)))


(defn bytes->hex ^String [^bytes input]
  (let [len (alength input)]
    (loop [sb (new StringBuilder)
           i 0]
      (if (= i len)
        (str sb)
        (let [b (aget input i)]
          (recur (.append sb (format "%02x" b)) (inc i)))))))


(defn bytes-count
  ^Integer [^String input ^String encoding]
  (-> input (str->bytes encoding) count))


(defn md5 ^bytes [^bytes input]
  (let [d (MessageDigest/getInstance "MD5")]
    (.update d input)
    (.digest d)))


(defn sha-256 ^bytes [^bytes input]
  (let [d (MessageDigest/getInstance "SHA-256")]
    (.update d input)
    (.digest d)))


(defn normalize-nfc [^String string]
  (Normalizer/normalize string Normalizer$Form/NFC))


(defn normalize-nfd [^String string]
  (Normalizer/normalize string Normalizer$Form/NFD))


(defn normalize-nfkd [^String string]
  (Normalizer/normalize string Normalizer$Form/NFKD))


(defn normalize-nfkc [^String string]
  (Normalizer/normalize string Normalizer$Form/NFKC))


(defn hmac-sha-256 [^bytes secret ^bytes message]
  (let [mac
        (Mac/getInstance "HmacSHA256")

        sks
        (new SecretKeySpec secret "HmacSHA256")]

    (.init mac sks)
    (.doFinal mac message)))
