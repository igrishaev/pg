(ns pg.copy
  (:import
   java.io.ByteArrayOutputStream
   java.io.InputStream
   java.io.OutputStream)
  (:require
   [clojure.java.io :as io]
   [pg.bytes.array :as array]
   [pg.encode.bin :as bin]
   [pg.oid :as oid]))


;;
;; Const
;;

(def ^{:tag 'bytes} HEADER
  (byte-array 11 (map int [\P \G \C \O \P \Y \newline 0xFF \return \newline 0])))


(def ^{:tag 'bytes} zero32
  (array/arr32 0))


(def ^{:tag 'bytes} -one32
  (array/arr32 -1))


(def ^{:tag 'bytes} -one16
  (array/arr16 -1))


;;
;; Misc
;;

(defn- coerce-oids [oids]
  (cond

    (map? oids)
    (reduce-kv
     (fn [acc k v]
       (assoc acc k (oid/->oid v)))
     {}
     oids)

    (sequential? oids)
    (into {} (map-indexed vector (map oid/->oid oids)))))


(defn enumerate [coll]
  (map-indexed vector coll))


;;
;; API
;;

(defn data->output-stream

  ([data out]
   (data->output-stream data out nil))

  ([data ^OutputStream out {:as opt :keys [oids]}]

   (let [oids (some-> oids coerce-oids)]
     (.write out HEADER)
     (.write out zero32)
     (.write out zero32)
     (doseq [row data]
       (.write out (array/arr16 (count row)))
       (doseq [[i item] (enumerate row)]
         (if (nil? item)
           (.write out -one32)
           (let [oid (get oids i)
                 buf (bin/encode item oid opt)]
             (.write out (array/arr32 (alength buf)))
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


(defn maps->data [maps row-keys]
  (map (apply juxt row-keys) maps))
