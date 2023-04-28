

(defn ->csv [chunk]
  (with-out-str
    (doseq [[a b c] chunk]
      (println a \, b \, c))))

(defn ->input-stream [^String text]
  (-> text
      (.getBytes "UTF-8")
      clojure.java.io/input-stream))

(-> chunk
    ->csv
    ->input-stream)
