(ns pg.client
  (:import
   clojure.lang.Keyword)
  (:require
   pg.client.auth.md5
   pg.client.auth.clear
   [pg.client.sql :as sql]
   [pg.client.impl.connection :as impl.connection]
   [pg.client.prot.connection :as prot.connection]))


(defn get-connection [config]

  {:arglists
   '([{:keys [host
              port
              user
              password
              database
              fn-notice
              fn-notification
              protocol-version]}])}

  (let [conn (impl.connection/connect config)]
    (prot.connection/authenticate conn)
    (prot.connection/initiate conn)))


(defmacro with-connection [[bind config] & body]
  `(let [~bind (impl.connection/connect ~config)]
     (try
       (prot.connection/authenticate ~bind)
       (prot.connection/initiate ~bind)
       ~@body
       (finally
         (prot.connection/terminate ~bind)))))


(defn query

  {:arglists
   '([conn sql]
     [conn sql {:keys [fn-column
                       fn-result
                       as-vectors?
                       as-maps?
                       as-java-maps?]}])}

  ([conn sql]
   (prot.connection/query conn sql nil))

  ([conn sql opt]
   (prot.connection/query conn sql opt)))


(defn begin [conn]
  (prot.connection/query conn "BEGIN" nil))


(defn rollback [conn]
  (prot.connection/query conn "ROLLBACK" nil))


(defn commit [conn]
  (prot.connection/query conn "COMMIT" nil))


(defn get-param ^String [conn ^String param]
  (prot.connection/get-parameter conn param))


(defn status ^Keyword [conn]
  (prot.connection/get-tx-status conn))


(defn pid [conn]
  (prot.connection/get-pid conn))


(defmacro with-tx

  {:arglists
   '([conn]
     [conn {:keys [read-only?
                   isolation-level
                   rollback?]}])}

  [[conn {:as opt :keys [rollback?]}]
   & body]
  `(do
     (begin ~conn)
     (let [[e# result#]
           (try
             [nil (do
                    ~(when opt
                       `(when-let [q# (sql/set-tx ~opt)]
                          (query ~conn q#)))
                    ~@body)]
             (catch Throwable e#
               [e# nil]))]
       (if e#
         (do
           (rollback ~conn)
           (throw e#))
         (do
           ~(if rollback?
              `(rollback ~conn)
              `(commit ~conn))
           result#)))))


(defn prepare [conn query]
  (prot.connection/parse conn query))


(defn close-statement [conn statement]
  (prot.connection/close-statement conn statement))


(defmacro with-prepare [[bind conn query] & body]
  `(let [conn# ~conn
         query# ~query
         ~bind (prepare conn# query#)]
     (try
       ~@body
       (finally
         (close-statement conn# ~bind)))))


(defn bind [conn statement params])


(defn execute [conn portal])


(defmacro with-statement [])


(defmacro with-portal [])
