(ns pg.client.copy
  (:import
   clojure.lang.RT)
  (:require
   [clojure.string :as str]
   [pg.client.conn :as conn]
   [pg.client.result :as res]
   [pg.encode.txt :as txt]))


(defn quote'' [string]
  (str/replace string #"\"" "\"\""))


(defn encode-row ^String [xs opt sep end]
  (let [iter (RT/iter xs)
        sb (new StringBuilder)]
    (loop []
      (when (.hasNext iter)
        (let [x (.next iter)]
          (when (some? x)
            (.append sb \")
            (.append sb (-> x (txt/encode nil opt) quote''))
            (.append sb \"))
          (when (.hasNext iter)
            (.append sb sep)))
        (recur)))
    (.append sb end)
    (.toString sb)))


(defn copy-in-rows [conn sql rows sep end]

  (let [iter
        (RT/iter rows)

        opt
        (conn/get-opt conn)

        encoding
        (conn/get-client-encoding conn)]

    (conn/send-query conn sql)

    (loop []
      (when (.hasNext iter)

        (let [row
              (.next iter)

              line
              (encode-row row opt sep end)

              buf
              (.getBytes line encoding)]

          (conn/send-copy-data conn buf)
          (recur))))

    (conn/send-copy-done conn)

    (res/interact conn :copy-in nil)))



(defn maps->rows [maps]
  )


(defn copy-in-maps

  ([conn sql maps]
   (copy-in-maps conn sql maps nil))

  ([conn sql maps opt]
   (let [rows (maps->rows maps)]

     )
))


(defn copy-in-stream [conn sql input-stream])

(defn copy-out-stream [conn sql output-stream])
