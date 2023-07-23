(ns pg.copy
  (:import
   java.io.ByteArrayOutputStream
   java.io.InputStream
   java.io.OutputStream)
  (:require
   [clojure.java.io :as io]
   [pg.bytes :as bytes]
   [pg.encode.bin :as bin]
   [pg.oid :as oid]))


;;
;; Const
;;

(def ^{:tag 'bytes} HEADER
  (byte-array 11 (map int [\P \G \C \O \P \Y \newline 0xFF \return \newline 0])))


(def ^{:tag 'bytes} zero32
  (bytes/int32->bytes 0))


(def ^{:tag 'bytes} -one32
  (bytes/int32->bytes -1))


(def ^{:tag 'bytes} -one16
  (bytes/int16->bytes -1))


;;
;; Misc
;;

(defn enumerate [coll]
  (map-indexed vector coll))


(defn- coerce-oids [oids]
  (cond

    (map? oids)
    (reduce-kv
     (fn [acc k v]
       (assoc acc k (oid/->oid v)))
     {}
     oids)

    (sequential? oids)
    (recur (into {} (enumerate oids)))))


;;
;; API
;;


(defn with-oids [data oids]
  (with-meta data {:pg/oids oids}))


(defn data->output-stream

  ([data out]
   (data->output-stream data out nil))

  ([data ^OutputStream out opt]
   (let [idx->oid
         (some-> (or (:oids opt)
                     (-> data meta :pg/oids))
                 (coerce-oids))]
     (.write out HEADER)
     (.write out zero32)
     (.write out zero32)
     (doseq [row data]
       (.write out (bytes/int16->bytes (count row)))
       (doseq [[i item] (enumerate row)]
         (if (nil? item)
           (.write out -one32)
           (let [oid (get idx->oid i)
                 buf (bin/encode item oid opt)]
             (.write out (bytes/int32->bytes (alength buf)))
             (.write out buf)))))
     (.write out -one16)
     (.close out))))


(defn data->bytes

  (^bytes [data]
   (data->bytes data nil))

  (^bytes [data opt]
   (with-open [out (new ByteArrayOutputStream)]
     (data->output-stream data out opt)
     (.toByteArray out))))


(defn data->file

  ([data ^String path]
   (data->file data path nil))

  ([data ^String path opt]
   (with-open [out (-> path
                       io/file
                       io/output-stream)]
     (data->output-stream data out opt))))


(defn data->input-stream
  (^InputStream [data]
   (data->input-stream data nil))

  (^InputStream [data opt]
   (-> data
       (data->bytes opt)
       io/input-stream)))


(defn maps->data
  [maps cols & [col->oid]]
  (let [idx->oid
        (when col->oid
          (map col->oid cols))]
    (cond-> (map (apply juxt cols) maps)
      idx->oid
      (with-oids idx->oid))))


(defn sqlvec-oids

  ([table]
   (sqlvec-oids table "public"))

  ([table schema]

   ["
SELECT
  ordinal_position, column_name, udt_name
FROM
  information_schema.columns
WHERE
  table_schema = ?
  AND table_name = ?
ORDER BY
  ordinal_position
"
    schema table]))
