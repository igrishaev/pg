(defproject com.github.igrishaev/pg "0.1.0-SNAPSHOT"

  :description
  "FIXME: write description"

  :url
  "http://example.com/FIXME"

  :license
  {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
   :url "https://www.eclipse.org/legal/epl-2.0/"}

  :plugins
  [[lein-sub "0.3.0"]]

  :dependencies
  []

  :sub
  ["pg-oid"
   "pg-encode"
   "pg-copy"]

  :managed-dependencies
  [[com.github.igrishaev/pg-oid "0.1.0-SNAPSHOT"]
   [com.github.igrishaev/pg-encode "0.1.0-SNAPSHOT"]]

  :profiles
  {:dev
   {:dependencies
    [[org.clojure/clojure "1.11.1"]]}})
