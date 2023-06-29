(ns pg.client2.conn
  (:refer-clojure :exclude [flush sync]))


(defn connect [config])

(defn set-pid [conn pid])

(defn get-pid [conn])

(defn set-secret-key [this secret-key])

(defn get-secret-key [this])

(defn set-tx-status [this tx-status])

(defn get-tx-status [this])

(defn set-parameter [this param value])

(defn get-parameter [this param])

(defn get-server-encoding [this])

(defn get-client-encoding ^String [this])

(defn get-password [this])

(defn get-user [this])

(defn sync [this])

(defn flush [this])

(defn read-message [this])

(defn read-messages [this])

(defn send-message [this message])

(defn authenticate [this])

(defn initiate [this])

(defn query [this str-sql opt])

(defn terminate [this])

(defn parse [this query])

(defn bind [this statement params])

(defn execute [this portal row-count])

(defn execute2 [this query params])

(defn close-statement [this statement-name])

(defn close-portal [this portal-name])

(defn describe-statement [this statement-name])

(defn describe-portal [this portal-name])
