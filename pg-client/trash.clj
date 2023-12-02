

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


  Options:
  - fn-result
  - fn-column
  - fn-unify
  - fn-keyval
  - fn-init
  - fn-reduce
  - fn-finalize


StartupMessage[protocolVersion=196608, user=ivan, database=ivan, options={}]
AuthenticationOk[]
ParameterStatus[param=application_name, value=]
ParameterStatus[param=client_encoding, value=UTF8]
ParameterStatus[param=DateStyle, value=ISO, MDY]
ParameterStatus[param=default_transaction_read_only, value=off]
ParameterStatus[param=in_hot_standby, value=off]
ParameterStatus[param=integer_datetimes, value=on]
ParameterStatus[param=IntervalStyle, value=postgres]
ParameterStatus[param=is_superuser, value=on]
ParameterStatus[param=server_encoding, value=UTF8]
ParameterStatus[param=server_version, value=14.5]
ParameterStatus[param=session_authorization, value=ivan]
ParameterStatus[param=standard_conforming_strings, value=on]
ParameterStatus[param=TimeZone, value=Europe/Moscow]
BackendKeyData[pid=42940, secretKey=843350714]
ReadyForQuery[txStatus=IDLE]
