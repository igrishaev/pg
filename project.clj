(defproject com.github.igrishaev/pg "0.1.12-SNAPSHOT"

  :description
  "Postgres stuff in pure Clojure"

  :url
  "https://github.com/igrishaev/pg"

  :license
  {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
   :url "https://www.eclipse.org/legal/epl-2.0/"}

  :deploy-repositories
  {"releases" {:url "https://repo.clojars.org" :creds :gpg}}

  :release-tasks
  [["vcs" "assert-committed"]
   ["sub" "change" "version" "leiningen.release/bump-version" "release"]
         ["change" "version" "leiningen.release/bump-version" "release"]
   ["vcs" "commit"]
   ["vcs" "tag" "--no-sign"]
   ["sub" "with-profile" "uberjar" "install"]
   ["sub" "with-profile" "uberjar" "deploy"]
   ["sub" "change" "version" "leiningen.release/bump-version"]
         ["change" "version" "leiningen.release/bump-version"]
   ["vcs" "commit"]
   ["vcs" "push"]]

  :plugins
  [[lein-sub "0.3.0"]
   [exoscale/lein-replace "0.1.1"]]

  :dependencies
  []

  :sub
  ["pg-common"
   "pg-types"
   "pg-joda-time"
   "pg-json"
   "pg-client"
   "pg-pool"
   "pg-honey"
   "pg-ssl"
   "pg-integration"]

  :managed-dependencies
  [[com.github.igrishaev/pg-common :version]
   [com.github.igrishaev/pg-types :version]
   [com.github.igrishaev/pg-joda-time :version]
   [com.github.igrishaev/pg-json :version]
   [com.github.igrishaev/pg-client :version]
   [com.github.igrishaev/pg-ssl :version]
   [com.github.igrishaev/pg-honey :version]
   [com.github.igrishaev/pg-integration :version]

   [org.clojure/clojure "1.11.1"]
   [joda-time/joda-time "2.12.5"]
   [clj-time "0.15.2"]
   [com.github.seancorfield/honeysql "2.4.1078"]
   [less-awful-ssl "1.0.6"]
   [com.github.seancorfield/next.jdbc "1.2.796"]
   [org.postgresql/postgresql "42.2.18"]
   [org.clojure/tools.logging "1.2.4"]
   [ch.qos.logback/logback-classic "1.4.5"]
   [com.stuartsierra/component "1.1.0"]
   [org.clojure/data.csv "1.0.1"]
   [metosin/jsonista "0.3.8"]
   [cheshire "5.11.0"]
   [criterium "0.4.6"]]

  :profiles
  {:dev
   {:source-paths ["./env/dev/src"]
    :resource-paths ["./env/dev/resources"]
    :dependencies
    [[org.clojure/clojure]]

    :global-vars
    {*warn-on-reflection* true
     *assert* true}}})
