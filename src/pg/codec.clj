(ns pg.codec
  (:import
   java.text.Normalizer
   java.text.Normalizer$Form
   javax.crypto.Mac
   javax.crypto.spec.SecretKeySpec
   java.security.MessageDigest
   java.util.Base64))


(defn b64-decode ^bytes [^bytes input]
  (.decode (Base64/getDecoder) input))


(defn b64-encode ^bytes [^bytes input]
  (.encode (Base64/getEncoder) input))


(defn bytes->str
  (^String [^bytes input]
   (new String input))

  (^String [^bytes input ^String encoding]
   (new String input encoding)))


(defn str->bytes
  (^bytes [^String input]
   (.getBytes input))

  (^bytes [^String input ^String encoding]
   (.getBytes input encoding)))


(defn bytes->hex ^String [^bytes input]
  (let [sb (new StringBuilder)]
    (doseq [b input]
      (.append sb (format "%02x" b)))
    (str sb)))


(defn md5 ^bytes [^bytes input]
  (let [d (MessageDigest/getInstance "MD5")]
    (.update d input)
    (-> d
        (.digest)
        (bytes->hex)
        (.toLowerCase)
        (.getBytes))))


(defn normalize-nfc [^String string]
  (Normalizer/normalize string Normalizer$Form/NFC))


(defn normalize-nfd [^String string]
  (Normalizer/normalize string Normalizer$Form/NFD))


(defn normalize-nfkd [^String string]
  (Normalizer/normalize string Normalizer$Form/NFKD))


(defn normalize-nfkc [^String string]
  (Normalizer/normalize string Normalizer$Form/NFKC))


(defn hmac-sha-256 [^bytes message ^bytes secret]
  (let [mac
        (Mac/getInstance "HmacSHA256")

        sks
        (new SecretKeySpec secret "HmacSHA256")]

    (.init mac sks)
    (.doFinal mac message)))
