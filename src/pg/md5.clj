(ns pg.md5
  (:import
   java.security.MessageDigest
   ;; javax.xml.bind.DatatypeConverter

   )
  )


(defn md5 [^bytes input]
  (let [d (MessageDigest/getInstance "MD5")]
    (.update d input)
    (-> d
        .digest
        ;; DatatypeConverter/printHexBinary
        ;; .toLowerCase
        ;; .getBytes

        )))
