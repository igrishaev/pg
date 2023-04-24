(ns pg.copy
  (:import
   java.io.ByteArrayOutputStream
   java.io.InputStream
   java.io.OutputStream)
  (:require
   [clojure.java.io :as io]
   [pg.bytes.array :as array]
   [pg.encode.bin :as bin]
   [pg.error :as e]))


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

(defn coerce-oids [oids]
  (cond

    (map? oids)
    oids

    (sequential? oids)
    (into {} (map-indexed vector oids))

    :else
    (e/error! "Wrong oids: %s" oids)))


(defn enumerate [coll]
  (map-indexed vector coll))


;;
;; API
;;

(defn table->out

  ([table out]
   (table->out table out nil))

  ([table ^OutputStream out {:as opt :keys [oids]}]

   (let [oids (some-> oids coerce-oids)]
     (.write out HEADER)
     (.write out zero32)
     (.write out zero32)
     (doseq [row table]
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


(defn table->bytes

  (^bytes [table]
   (table->bytes table nil))

  (^bytes [table opt]
   (with-open [out (new ByteArrayOutputStream)]
     (table->out table out opt)
     (.toByteArray out))))


(defn table->file

  ([table ^String path]
   (table->file table path nil))

  ([table ^String path opt]
   (with-open [out (-> path
                       io/file
                       io/output-stream)]
     (table->out table out opt))))


(defn table->input-stream
  (^InputStream [table]
   (table->input-stream table nil))

  (^InputStream [table opt]
   (-> table
       (table->bytes opt)
       io/input-stream)))


(defn maps->table [maps row-keys]
  (map (apply juxt row-keys) maps))
