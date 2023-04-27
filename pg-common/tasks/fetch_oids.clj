(ns fetch-oids
  (:import
   java.net.URL)
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.java.io :as io])

  )


(def URL_RAW
  "https://raw.githubusercontent.com/postgres/postgres/master/src/include/catalog/pg_type.dat")


#_
{typcategory "N"
 typbyval "t"
 typinput "int4in"
 typsend "int4send"
 array_type_oid "1007"
 typoutput "int4out"
 typlen "4"
 typname "int4"
 typreceive "int4recv"
 oid "23"
 descr "-2 billion to 2 billion integer, 4-byte storage"
 typalign "i"}


(defn -main [& _]
  (let [content
        (-> (new URL URL_RAW)
            (slurp)
            (str/replace #"#.*" "")
            (str/replace #"'" "\"")
            (str/replace #"=>" "")
            (edn/read-string))]

    content

    )
  )
