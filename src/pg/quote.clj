(ns pg.quote
  (:require
   [clojure.string :as str]))


(defn quote-text [string]
  (let [len (count string)
        sb  (new StringBuilder)]
    (.append sb \')
    (loop [i 0]
      (if (= i len)
        (do
          (.append sb \')
          (str sb))
        (let [c (get string i)
              c*
              (case c
                \tab       "\\t"
                \formfeed  "\\f"
                \newline   "\\n"
                \return    "\\r"
                \backspace "\\b"
                \'         "''"
                \\         "\\\\"
                c)]
          (.append sb c*)
          (recur (inc i)))))))
