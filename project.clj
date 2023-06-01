(defproject com.github.igrishaev/pg "0.1.1-SNAPSHOT"

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
   ["sub" "test"]
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
  [[com.github.igrishaev/pg-common]
   [com.github.igrishaev/pg-encode]
   [com.github.igrishaev/pg-copy]
   [com.github.igrishaev/pg-copy-jdbc]
   [com.github.igrishaev/pg-joda-time]
   [com.github.igrishaev/pg-client]]

  :sub
  ["pg-common"
   "pg-encode"
   "pg-decode"
   "pg-copy"
   "pg-copy-jdbc"
   "pg-joda-time"
   "pg-client"]

  :managed-dependencies
  [[com.github.igrishaev/pg-common :version]
   [com.github.igrishaev/pg-encode :version]
   [com.github.igrishaev/pg-decode :version]
   [com.github.igrishaev/pg-copy :version]
   [com.github.igrishaev/pg-copy-jdbc :version]
   [com.github.igrishaev/pg-joda-time :version]
   [com.github.igrishaev/pg-client :version]
   [joda-time/joda-time "2.12.5"]
   [com.github.seancorfield/next.jdbc "1.2.796"]
   [org.postgresql/postgresql "42.2.18"]]

  :profiles
  {:dev
   {:dependencies
    [[org.clojure/clojure "1.11.1"]]

    :global-vars
    {*warn-on-reflection* true
     *assert* true}}})
