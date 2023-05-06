(defproject com.github.igrishaev/pg "0.1.0-SNAPSHOT"

  :description
  "Postgres stuff in pure Clojure"

  :url
  "https://github.com/igrishaev/pg"

  :license
  {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
   :url "https://www.eclipse.org/legal/epl-2.0/"}

  :release-tasks
  [["vcs" "assert-committed"]
   ["test"]
   ["change" "version" "leiningen.release/bump-version" "release"]
   ["vcs" "commit"]
   ["vcs" "tag" "--no-sign"]
   ["deploy"]
   ["change" "version" "leiningen.release/bump-version"]
   ["vcs" "commit"]
   ["vcs" "push"]]

  :plugins
  [[lein-sub "0.3.0"]
   [exoscale/lein-replace "0.1.1"]]

  :dependencies
  [[com.github.igrishaev/pg-common :version]
   [com.github.igrishaev/pg-encode :version]
   [com.github.igrishaev/pg-copy :version]
   [com.github.igrishaev/pg-copy-jdbc :version]
   [com.github.igrishaev/pg-joda-time :version]]

  :sub
  ["pg-common"
   "pg-encode"
   "pg-copy"
   "pg-copy-jdbc"
   "pg-joda-time"]

  :managed-dependencies
  [[com.github.igrishaev/pg-common :version]
   [com.github.igrishaev/pg-encode :version]
   [com.github.igrishaev/pg-copy :version]
   [com.github.igrishaev/pg-copy-jdbc :version]
   [com.github.igrishaev/pg-joda-time :version]
   [joda-time/joda-time "2.12.5"]
   [com.github.seancorfield/next.jdbc "1.2.796"]
   [org.postgresql/postgresql "42.2.18"]]

  :profiles
  {:dev
   {:dependencies
    [[org.clojure/clojure "1.11.1"]]}})
