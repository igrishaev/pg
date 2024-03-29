(ns fetch-oids
  (:import
   java.io.File
   java.net.URL)
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.pprint :as pprint]
   [clojure.java.io :as io]))

(def URL_RAW
  "https://raw.githubusercontent.com/postgres/postgres/master/src/include/catalog/pg_type.dat")

(def ^String FILE_DAT
  "../pg_type.dat")

(defn load-from-url []
  (slurp (new URL URL_RAW)))

(defn load-from-file []
  (slurp (new File FILE_DAT)))

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
        (-> (load-from-file) ;; (load-from-url)
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

        array-oids
        (->> content
             (map (fn [info]
                    (get info 'array_type_oid)))
             (filter int?))


        array->oid
        (into {} (for [{:syms [oid
                               typname
                               array_type_oid]} content
                       :when (and oid array_type_oid)]
                   [array_type_oid typname]))

        name->oid
        (reduce
         (fn [acc {:syms [typname oid array_type_oid]}]
           (cond-> acc
             oid
             (assoc typname oid)
             array_type_oid
             (assoc (format "_%s" typname) array_type_oid)))
         {}
         content)

        oid->name
        (into {} (for [[k v] name->oid]
                   [v k]))]

    (println ";; Mostly machine-generated, see `fetch_oids.clj`")
    (println "(ns pg.oid")
    (println "  (:refer-clojure :exclude [char name time]))")

    (println)
    (println)

    (doseq [{:syms [oid
                    typname
                    descr
                    array_type_oid]} content]

      (println (format "(def ^int %-30s %4d)" typname oid))

      (when array_type_oid
        (println (format "(def ^int _%-29s %4d)" typname array_type_oid))))

    (println)
    (println)

    (println "(def ^:private -name->oid {")
    (doseq [[k _] name->oid]
      (println (format "  %-29s %s" (str \" k \") k)))
    (println "})")

    (println)

    (println "(def array-oids #{")
    (doseq [oid array-oids]
      (println (format "  %s" (get oid->name oid))))
    (println "})")

    (println)

    (println "(def ^:private -array->oid {")
    (doseq [[k v] array->oid]
      (println (format "  %-15s %s" (get oid->name k) v)))
    (println "})")


    (println)

    (println "
(defn name->oid [^String oid-name]
  (get -name->oid oid-name))
")


    (println "
(defn array->oid [array-oid]
  (get -array->oid array-oid))
")

    (println "
(defn ->oid [x]
  (cond
    (int? x) x
    (string? x) (name->oid x)))
")

    (println)))
