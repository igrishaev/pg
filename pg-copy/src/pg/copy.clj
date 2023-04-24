(ns pg.copy
  (:import
   java.io.ByteArrayOutputStream
   java.io.InputStream
   java.io.OutputStream)
  (:require
   [clojure.java.io :as io]
   [pg.bytes.array :as array]
   [pg.encode.bin :as bin]))


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
;; API
;;

;; TODO: oids

(defn table->out [table ^OutputStream out]
  (.write out HEADER)
  (.write out zero32)
  (.write out zero32)
  (doseq [row table]
    (.write out (array/arr16 (count row)))
    (doseq [item row]
      (if (nil? item)
        (.write out -one32)
        (let [buf (bin/encode item)]
          (.write out (array/arr32 (alength buf)))
          (.write out buf)))))
  (.write out -one16)
  (.close out))


(defn table->bytes ^bytes [table]
  (with-open [out (new ByteArrayOutputStream)]
    (table->out table out)
    (.toByteArray out)))


(defn table->file [table ^String path]
  (with-open [out (-> path
                      io/file
                      io/output-stream)]
    (table->out table out)))


(defn table->input-stream ^InputStream [table]
  (-> table
      table->bytes
      io/input-stream))


(defn maps->table [maps row-keys]
  (map (apply juxt row-keys) maps))
