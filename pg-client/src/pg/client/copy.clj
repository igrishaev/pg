(ns pg.client.copy
  (:import
   clojure.lang.RT
   java.io.InputStream
   java.util.Iterator)
  (:require
   [clojure.string :as str]
   [pg.bytes :as bytes]
   [pg.client.conn :as conn]
   [pg.client.result :as res]
   [pg.encode.txt :as txt]))


(def ^{:tag 'bytes} BIN_HEADER
  (let [characters
        [\P \G \C \O \P \Y \newline 0xFF \return \newline 0]]
    (byte-array (count characters) (map int characters))))


(defn csv-quote [string]
  (str/replace string #"\"" "\"\""))


(defn csv-encode ^String [row opt oids sep end]
  (let [iter (RT/iter row)
        sb (new StringBuilder)]
    (loop [i 0]
      (when (.hasNext iter)
        (let [x (.next iter)
              oid (get oids i)]
          (when (some? x)
            (.append sb \")
            (.append sb (-> x (txt/encode nil opt) csv-quote))
            (.append sb \"))
          (when (.hasNext iter)
            (.append sb sep)))
        (recur (inc i))))
    (.append sb end)
    (.toString sb)))


(defn bin-encode ^bytes [row oids]
  )


(defn copy-in-csv [conn rows oids sep end]

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
              (csv-encode row opt oids sep end)

              buf
              (.getBytes line encoding)]

          (conn/send-copy-data conn buf)
          (recur))))))


;; TODO: iter
(defn copy-in-bin [conn rows oids]

  (let [len
        (count rows)

        iter
        (RT/iter rows)

        opt
        (conn/get-opt conn)

        encoding
        (conn/get-client-encoding conn)]

    (conn/send-copy-data conn BIN_HEADER)
    ;; zero32
    ;; zero32

    ;; rows...
    ;; -one16


    (loop []
      (when (.hasNext iter)
        ))))


(defn copy-in-rows [conn sql rows binary? oids sep end]

  (let [iter
        (RT/iter rows)

        opt
        (conn/get-opt conn)

        encoding
        (conn/get-client-encoding conn)]

    (conn/send-query conn sql)

    (if binary?
      (copy-in-bin conn rows oids)
      (copy-in-csv conn rows oids sep end))

    (conn/send-copy-done conn)
    (res/interact conn :copy-in nil)))


(defn maps->rows [maps fields]
  (let [selector (apply juxt fields)]
    (map selector maps)))


(defn oids-maps->rows [oids-map fields]
  (let [selector (apply juxt fields)]
    (selector oids-map)))


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
