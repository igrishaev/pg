(defproject com.github.igrishaev/pg-client2 "0.1.11-SNAPSHOT"

  :description
  "Postgres client in pure Clojure^W Java (no JDBC)"

  :plugins
  [[lein-parent "0.3.8"]]

  :dependencies
  [[org.clojure/clojure]
   [metosin/jsonista]]

  :java-source-paths ["src"]
  :javac-options ["-Xlint:unchecked"
                  "-Xlint:preview"
                  "--release" "16"]

  :parent-project
  {:path "../project.clj"
   :inherit [:deploy-repositories
             :license
             :release-tasks
             :managed-dependencies
             :plugins
             :repositories
             :url
             [:profiles :dev]]}

  :profiles
  {:test
   {:dependencies
    [[com.github.seancorfield/next.jdbc]
     [org.postgresql/postgresql]
     [org.clojure/data.csv]
     [criterium]]}})
