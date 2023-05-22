

(time
 (client/with-connection [conn CONFIG]
   (doseq [_ (range 100000)]
     (client/query conn "select 42 as foo"))))


(time
 (let [conn (next.jdbc/get-connection {:dbtype "postgres" :dbname "ivan" :user "ivan" :password "ivan" :port 15432})]
   (doseq [_ (range 100000)]
     (next.jdbc/execute! conn ["select 42 as foo"]))))


;; 8
;; {:type :AuthenticationGSSContinue
;;  :status status
;;  :auth (bb/read-rest bb)}

;; 9
;; {:type :AuthenticationSSPI
;;  :status status}

;; 10
;; {:type :AuthenticationSASL
;;  :status status
;;  :sasl-types
;;  (loop [acc #{}]
;;    (let [item
;;          (-> bb
;;              bb/read-cstring
;;              codec/bytes->str)]
;;      (if (= item "")
;;        acc
;;        (recur (conj acc item)))))}

;; 11
;; {:type :AuthenticationSASLContinue
;;  :status status
;;  :server-first-message
;;  (-> bb
;;      bb/read-rest
;;      codec/bytes->str)}

;; 12
;; {:type :AuthenticationSASLFinal
;;  :status status
;;  :server-final-message
;;  (-> bb
;;      bb/read-rest
;;      codec/bytes->str)}
