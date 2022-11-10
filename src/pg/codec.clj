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
  (let [len (alength input)]
    (loop [sb (new StringBuilder)
           i 0]
      (if (= i len)
        (str sb)
        (let [b (aget input i)]
          (recur (.append sb (format "%02x" b)) (inc i)))))))


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


(defn hmac-sha-256 [^bytes message ^bytes secret]
  (let [mac
        (Mac/getInstance "HmacSHA256")

        sks
        (new SecretKeySpec secret "HmacSHA256")]

    (.init mac sks)
    (.doFinal mac message)))


#_
(-> "MXf1hERKrJWAQSlcYSRe6A=="
                                           str->bytes
                                           b64-decode
                                           (concat-bytes (byte-array [0 0 0 1])))


#_
(-> (hmac-sha-256 (.getBytes "aaa") (.getBytes "bbb"))
    (bytes->hex)
    )

#_ db404a4b73d69c3dd753825b49266f5651a2d965a70daf12bfdb8727ec2c10b0

#_
(codec/bytes->hex (Hi "secret" (-> "MXf1hERKrJWAQSlcYSRe6A==" codec/str->bytes codec/b64-decode) 4096))


(defn xor-bytes ^bytes [^bytes bytes1 ^bytes bytes2]

  (when-not (= (alength bytes1) (alength bytes2))
    (throw (ex-info "XOR error: the lengths do not match")))

  (let [len
        (alength bytes1)]

    (loop [result (byte-array len)
           i 0]

      (if (= i len)
        result

        (let [b1 (aget bytes1 i)
              b2 (aget bytes2 i)]
          (aset result i ^Byte (bit-xor b1 b2))
          (recur result (inc i)))))))


(defn concat-bytes ^bytes [^bytes bytes1 ^bytes bytes2]
  (let [result
        (byte-array (+ (alength bytes1) (alength bytes2)))]
    (System/arraycopy bytes1 0 result 0                (alength bytes1))
    (System/arraycopy bytes2 0 result (alength bytes1) (alength bytes2))
    result))
