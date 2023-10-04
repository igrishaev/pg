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
   [pg.client.result :as res]
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
              (.append sb (-> x (txt/encode nil opt) csv-quote))
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


(defn copy-in-rows [conn sql rows binary? oids sep end null]

  (let [opt
        (conn/get-opt conn)

        encoding
        (conn/get-client-encoding conn)]

    (conn/send-query conn sql)

    (if binary?
      (copy-in-bin conn rows oids)
      (copy-in-csv conn rows oids sep end null))

    (conn/send-copy-done conn)
    (res/interact conn :copy-in nil)))


(defn maps->rows [maps fields]
  (let [selector (apply juxt fields)]
    (map selector maps)))


(defn oids-maps->rows [oids-map fields]
  (let [selector (apply juxt fields)]
    (selector oids-map)))


(defn copy-in-maps [conn sql maps fields binary? oids sep end]
  (let [rows (maps->rows maps fields)
        oids (oids-maps->rows oids)]
    (copy-in-rows conn sql rows binary? oids sep end)))


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
    (res/interact conn :copy-in nil)))
