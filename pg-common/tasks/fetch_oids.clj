(ns fetch-oids
  (:import
   java.net.URL)
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.java.io :as io]))


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
            (edn/read-string))

        content
        (map (fn [info]
               (-> info
                   (update 'typname name)
                   (update 'oid parse-long)
                   (update 'array_type_oid (fnil parse-long ""))))
             content)

        name->oid
        (reduce
         (fn [acc {:syms [typname oid array_type_oid]}]
           (cond-> acc
             oid
             (assoc typname oid)
             array_type_oid
             (assoc (format "_%s" typname) array_type_oid)))
         {}
         content)]

    (println "(ns pg.oid)")
    (println)
    (println)

    (doseq [{:syms [oid
                    typname
                    descr
                    array_type_oid]} content]

      (println (format "(defn ^int %-30s %4d)" typname oid))

      (when array_type_oid
        (println (format "(defn ^int _%-29s %4d)" typname array_type_oid))))

    (println)

    (printf "
(def ^:private -name->oid
  %s) "
            (with-out-str
              (clojure.pprint/pprint name->oid)))

    (println)

    (println "
(defn name->oid ^int [^String oid-name]
  (get -name->oid oid-name))
")))
