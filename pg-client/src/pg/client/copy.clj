(ns pg.client.copy
  (:import
   clojure.lang.RT
   java.io.InputStream
   java.util.Iterator)
  (:require
   [clojure.string :as str]
   [pg.bytes :as bytes]
   [pg.out :as out]
   [pg.client.conn :as conn]
   [pg.client.flow :as flow]
   [pg.encode.txt :as txt]
   [pg.encode.bin :as bin]))


(def ^bytes BUF_HEADER
  (let [characters
        [\P \G \C \O \P \Y \newline 0xFF \return \newline 0 ;; lead
         0 0 0 0 ;; int32 zero
         0 0 0 0 ;; int32 zero
         ]]
    (byte-array (count characters) (map int characters))))


(defn csv-quote [string]
  (str/replace string #"\"" "\"\""))


(defn csv-encode ^String [row opt oids sep end null]
  (let [iter (RT/iter row)
        sb (new StringBuilder)]
    (loop [i 0]
      (when (.hasNext iter)
        (let [x (.next iter)
              oid (get oids i)]
          (if (some? x)
            (do
              (.append sb \")
              (.append sb (-> x (txt/encode oid opt) csv-quote))
              (.append sb \"))
            (.append sb null))
          (when (.hasNext iter)
            (.append sb sep)))
        (recur (inc i))))
    (.append sb end)
    (.toString sb)))


(defn bin-encode ^bytes [row oids opt]

  (let [out
        (out/create)

        iter
        (RT/iter row)

        len
        (count row)]

    (out/write-bytes out (bytes/int16->bytes len))

    (loop [i 0]
      (when (.hasNext iter)
        (let [x (.next iter)]
          (if (nil? x)
            (out/write-bytes out bytes/-one32)
            (let [oid (get oids i)
                  buf (bin/encode x oid opt)]
              (out/write-bytes out (bytes/int32->bytes (alength buf)))
              (out/write-bytes out buf))))
        (recur (inc i))))

    (out/array out)))


(defn copy-in-csv [conn rows oids sep end null]

  (let [iter
        (RT/iter rows)

        opt
        (conn/get-opt conn)

        encoding
        (conn/get-client-encoding conn)]

    (loop []
      (when (.hasNext iter)

        (let [row
              (.next iter)

              line
              (csv-encode row opt oids sep end null)

              buf
              (.getBytes line encoding)]

          (conn/send-copy-data conn buf)
          (recur))))))


(defn copy-in-bin [conn rows oids]

  (let [iter
        (RT/iter rows)

        opt
        (conn/get-opt conn)

        encoding
        (conn/get-client-encoding conn)]

    (conn/send-copy-data conn BUF_HEADER)

    (loop []
      (when (.hasNext iter)
        (let [row (.next iter)
              buf (bin-encode row oids opt)]
          (conn/send-copy-data conn buf))
        (recur)))

    (conn/send-copy-data conn bytes/-one16)))


(defn copy-in-rows [conn sql rows format oids sep end null]

  (let [opt
        (conn/get-opt conn)

        encoding
        (conn/get-client-encoding conn)]

    (conn/send-query conn sql)

    (case format
      :csv
      (copy-in-csv conn rows oids sep end null)
      :bin
      (copy-in-bin conn rows oids)
      :txt
      (throw (new Exception "Text COPY format is not implemented yet"))
      ;; else
      (throw (new Exception "wrong COPY format")))

    (conn/send-copy-done conn)
    (flow/interact conn :copy-in nil)))


(defn maps->keys [maps]
  (some-> maps first keys vec))


(defn copy-in-maps [conn sql maps keys format oids sep end null]
  (let [keys
        (or keys (maps->keys maps))

        selector
        (if (seq keys)
          (apply juxt keys)
          (constantly nil))

        rows
        (map selector maps)

        oids
        (selector oids)]

    (copy-in-rows conn sql rows format oids sep end null)))


(defn copy-in-stream
  [conn sql ^InputStream input-stream buffer-size]

  (conn/send-query conn sql)

  (let [buf (byte-array buffer-size)]

    (loop []
      (let [read (.read input-stream buf)]
        (when-not (neg? read)
          (if (= read buffer-size)
            (conn/send-copy-data conn buf)
            (let [slice (bytes/slice buf 0 read)]
              (conn/send-copy-data conn slice)))
          (recur))))

    (conn/send-copy-done conn)
    (flow/interact conn :copy-in nil)))
