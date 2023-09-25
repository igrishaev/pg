(ns pg.ssl
  (:require
   [less.awful.ssl :as ssl])
  (:import
   javax.net.ssl.SSLContext))


(defn context ^SSLContext
  [{:keys [^String key-file
           ^String cert-file
           ^String ca-cert-file]}]
  (if ca-cert-file
    (ssl/ssl-context key-file cert-file ca-cert-file)
    (ssl/ssl-context key-file cert-file)))


(defn ssl-context-reader [mapping]
  `(context ~mapping))
